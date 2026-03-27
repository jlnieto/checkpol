document.addEventListener("DOMContentLoaded", () => {
    const detailRoot = document.querySelector("[data-booking-detail]");
    if (!detailRoot) {
        return;
    }

    const feedbackNode = detailRoot.querySelector("[data-review-feedback]");

    detailRoot.addEventListener("click", async (event) => {
        const copyButton = event.target.closest("[data-copy-share-message]");
        if (!copyButton) {
            return;
        }

        const messageNode = detailRoot.querySelector("[data-share-message-text]");
        if (!messageNode) {
            return;
        }

        try {
            await navigator.clipboard.writeText(messageNode.textContent.trim());
            showFeedback("Mensaje copiado.", "success");
        } catch (error) {
            showFeedback("No se pudo copiar el mensaje. Copialo manualmente.", "error");
        }
    });

    detailRoot.addEventListener("submit", async (event) => {
        const form = event.target.closest("[data-review-form]");
        if (!form) {
            return;
        }

        event.preventDefault();

        const button = form.querySelector("[data-review-button]");
        const originalLabel = button ? button.textContent : "";
        if (button) {
            button.disabled = true;
            button.classList.add("review-button-loading");
            button.textContent = "Marcando...";
        }

        try {
            const response = await fetch(form.action, {
                method: "POST",
                body: new FormData(form),
                headers: {
                    "X-Requested-With": "fetch",
                    "Accept": "application/json"
                },
                credentials: "same-origin"
            });
            const payload = await response.json().catch(() => ({}));

            if (!response.ok || payload.success === false) {
                throw new Error(payload.message || "No se pudo marcar como revisado. Intentalo de nuevo.");
            }

            await refreshDetailSections();
            showFeedback(payload.message || "Huesped marcado como revisado.", "success");
        } catch (error) {
            if (button) {
                button.disabled = false;
                button.classList.remove("review-button-loading");
                button.textContent = originalLabel;
            }
            showFeedback(error.message || "No se pudo marcar como revisado. Intentalo de nuevo.", "error");
        }
    });

    async function refreshDetailSections() {
        const response = await fetch(window.location.pathname, {
            headers: {
                "X-Requested-With": "fetch"
            },
            credentials: "same-origin"
        });
        const html = await response.text();
        const parser = new DOMParser();
        const nextDocument = parser.parseFromString(html, "text/html");

        syncSection("[data-owner-top-stack]", nextDocument);
        syncSection("[data-guests-section]", nextDocument);
        syncSection("[data-owner-secondary]", nextDocument, "[data-guests-section]");
        syncSection("[data-files-section]", nextDocument, "[data-owner-secondary]");
    }

    function syncSection(selector, nextDocument, insertAfterSelector) {
        const currentNode = detailRoot.querySelector(selector);
        const nextNode = nextDocument.querySelector(selector);

        if (currentNode && nextNode) {
            currentNode.replaceWith(nextNode);
            return;
        }

        if (currentNode && !nextNode) {
            currentNode.remove();
            return;
        }

        if (!currentNode && nextNode && insertAfterSelector) {
            const anchor = detailRoot.querySelector(insertAfterSelector);
            if (anchor) {
                anchor.after(nextNode);
            }
        }
    }

    function showFeedback(message, kind) {
        if (!feedbackNode) {
            return;
        }

        feedbackNode.hidden = false;
        feedbackNode.className = "detail-inline-feedback detail-inline-feedback-" + kind;
        feedbackNode.textContent = message;

        window.clearTimeout(showFeedback.timeoutId);
        showFeedback.timeoutId = window.setTimeout(() => {
            feedbackNode.hidden = true;
            feedbackNode.textContent = "";
            feedbackNode.className = "detail-inline-feedback";
        }, 2600);
    }
});
