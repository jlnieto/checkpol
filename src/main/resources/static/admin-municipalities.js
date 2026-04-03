document.addEventListener("DOMContentLoaded", () => {
  bindSubmitLocks();
  focusPreviewPanel();
});

function bindSubmitLocks() {
  document.querySelectorAll("[data-admin-submit-lock]").forEach((form) => {
    if (!(form instanceof HTMLFormElement)) {
      return;
    }

    form.addEventListener("submit", (event) => {
      const submitter = event.submitter;
      if (!(submitter instanceof HTMLButtonElement || submitter instanceof HTMLInputElement)) {
        return;
      }

      if (form.dataset.submitting === "true") {
        event.preventDefault();
        return;
      }

      form.dataset.submitting = "true";
      form.classList.add("mono-form-busy");
      form.setAttribute("aria-busy", "true");

      form.querySelectorAll('button[type="submit"], input[type="submit"]').forEach((button) => {
        if (button instanceof HTMLButtonElement || button instanceof HTMLInputElement) {
          button.disabled = true;
        }
      });

      const loadingLabel = submitter.dataset.loadingLabel;
      if (loadingLabel) {
        if (submitter instanceof HTMLButtonElement) {
          submitter.dataset.originalLabel = submitter.innerHTML;
          submitter.textContent = loadingLabel;
        } else {
          submitter.dataset.originalLabel = submitter.value;
          submitter.value = loadingLabel;
        }
      }
    });
  });
}

function focusPreviewPanel() {
  const previewPanel = document.querySelector("[data-preview-panel]");
  if (!(previewPanel instanceof HTMLElement)) {
    return;
  }

  previewPanel.scrollIntoView({ behavior: "smooth", block: "start" });

  const focusTarget = previewPanel.querySelector("[data-preview-focus]");
  if (focusTarget instanceof HTMLElement) {
    window.setTimeout(() => {
      focusTarget.focus({ preventScroll: true });
    }, 250);
  }
}
