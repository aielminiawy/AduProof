'use strict';

const $ = (id) => document.getElementById(id);
const ui = {
  publish: $('publish-form'), verify: $('verify-form'),
  issued: $('issued-box'), result: $('result-panel'), toast: $('toast')
};
let demoToken = null;
let mostRecentNotice = null;

function say(message) { ui.toast.textContent = message; }

async function api(path, data) {
  const options = { headers: {} };
  if (data) {
    options.method = 'POST';
    options.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
    options.headers['X-AduProof-Demo-Token'] = demoToken;
    options.body = new URLSearchParams(data);
  }
  const response = await fetch(path, options);
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || `Request failed (${response.status})`);
  return result;
}

function copyOriginal(notice) {
  $('verify-id').value = notice.id;
  $('verify-title').value = notice.title;
  $('verify-body').value = notice.body;
  $('verify-destination').value = notice.destination;
  ui.result.hidden = true;
}

function markResult(data) {
  const status = data.status;
  const names = {
    MATCH: 'Exact match confirmed', MISMATCH: 'The copy has changed',
    REVOKED: 'Approval was withdrawn', EXPIRED: 'This notice has expired',
    INVALID_PROOF: 'This proof is invalid', NOT_FOUND: 'No signed record found'
  };
  ui.result.dataset.kind = status.toLowerCase();
  $('result-title').textContent = names[status] || 'Unable to verify';
  $('result-explanation').textContent = data.explanation;
  const grid = $('match-grid');
  grid.replaceChildren();
  if (status !== 'NOT_FOUND' && status !== 'INVALID_PROOF') {
    for (const [title, matches] of Object.entries({
      Title: data.titleMatches, Message: data.bodyMatches, Link: data.destinationMatches
    })) {
      const chip = document.createElement('span');
      chip.textContent = `${title}: ${matches ? 'unchanged' : 'different'}`;
      grid.appendChild(chip);
    }
  }
  ui.result.hidden = false;
}

ui.publish.addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = $('publish-button');
  button.disabled = true;
  say('');
  try {
    const data = Object.fromEntries(new FormData(ui.publish));
    const result = await api('/api/publish', data);
    mostRecentNotice = result;
    $('notice-id').value = result.id;
    $('proof-token').value = result.proof;
    $('notice-meta').textContent = `Local demo publisher · Expires ${new Date(result.expiresAt).toLocaleString()}`;
    $('revoke-button').disabled = false;
    ui.issued.hidden = false;
    copyOriginal(result);
    say('Signed notice created. Try verifying the original, then change its link.');
  } catch (error) { say(error.message); }
  finally { button.disabled = false; }
});

ui.verify.addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = $('verify-button');
  button.disabled = true;
  say('');
  try {
    const result = await api('/api/verify', Object.fromEntries(new FormData(ui.verify)));
    markResult(result);
  } catch (error) { say(error.message); }
  finally { button.disabled = false; }
});

$('load-original').addEventListener('click', () => {
  if (!mostRecentNotice) { say('Sign a notice first, or paste a notice ID.'); return; }
  copyOriginal(mostRecentNotice);
  say('Original signed fields loaded. Click Check this copy.');
});

$('tamper-link').addEventListener('click', () => {
  if (!mostRecentNotice) { say('Sign a notice first.'); return; }
  copyOriginal(mostRecentNotice);
  const original = $('verify-destination').value;
  try {
    const changed = new URL(original);
    changed.hostname = 'modified-link.example';
    $('verify-destination').value = changed.toString();
    say('The received link has been altered. Click Check this copy to compare.');
  } catch { say('Please supply a valid original URL first.'); }
});

$('revoke-button').addEventListener('click', async () => {
  if (!mostRecentNotice) return;
  try {
    await api('/api/revoke', {id: mostRecentNotice.id});
    $('revoke-button').disabled = true;
    say('Approval withdrawn. Verify the original again to see REVOKED.');
  } catch (error) { say(error.message); }
});

$('copy-id').addEventListener('click', async () => {
  try { await navigator.clipboard.writeText($('notice-id').value); say('Notice ID copied.'); }
  catch { $('notice-id').select(); say('Copy the selected notice ID manually.'); }
});

(async function initialize() {
  try {
    const info = await api('/api/info');
    demoToken = info.token;
    const url = new URL(window.location.href);
    const id = url.searchParams.get('id');
    if (id) {
      const data = await api('/api/notice?id=' + encodeURIComponent(id));
      copyOriginal(data);
      say('Signed record loaded. For a meaningful check, compare it with your received copy.');
    }
  } catch (error) { say('Local demo failed to initialize: ' + error.message); }
})();
