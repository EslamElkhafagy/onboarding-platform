/** Opens a fetched file blob in a new browser tab, revoking the temporary URL afterwards. */
export function openBlobInNewTab(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  const win = window.open(url, '_blank');
  // If a popup blocker stopped the tab, fall back to navigating the current one.
  if (!win) {
    window.location.href = url;
  }
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
