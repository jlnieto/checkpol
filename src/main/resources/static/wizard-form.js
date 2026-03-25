document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-wizard-form]").forEach((form) => {
        const panels = Array.from(form.querySelectorAll("[data-step-panel]"));
        const indicators = Array.from(form.querySelectorAll("[data-step-indicator]"));
        const progressLabel = form.querySelector("[data-step-progress]");
        if (panels.length === 0) {
            return;
        }

        const setActiveStep = (index) => {
            panels.forEach((panel, panelIndex) => {
                const active = panelIndex === index;
                panel.hidden = !active;
                panel.classList.toggle("wizard-panel-active", active);
            });

            indicators.forEach((indicator, indicatorIndex) => {
                indicator.classList.toggle("wizard-step-active", indicatorIndex === index);
                indicator.classList.toggle("wizard-step-done", indicatorIndex < index);
            });

            if (progressLabel) {
                progressLabel.textContent = `Paso ${index + 1} de ${panels.length}`;
            }

            form.dataset.activeStep = String(index);
        };

        const findFirstErrorStep = () => {
            const fieldError = form.querySelector(".error:not(:empty), .error-box");
            if (!fieldError) {
                return 0;
            }
            const panel = fieldError.closest("[data-step-panel]");
            if (!panel) {
                return 0;
            }
            return panels.indexOf(panel);
        };

        setActiveStep(findFirstErrorStep());

        form.querySelectorAll("[data-step-next]").forEach((button) => {
            button.addEventListener("click", () => {
                const currentStep = Number(form.dataset.activeStep || "0");
                const nextStep = Math.min(currentStep + 1, panels.length - 1);
                setActiveStep(nextStep);
            });
        });

        form.querySelectorAll("[data-step-prev]").forEach((button) => {
            button.addEventListener("click", () => {
                const currentStep = Number(form.dataset.activeStep || "0");
                const prevStep = Math.max(currentStep - 1, 0);
                setActiveStep(prevStep);
            });
        });
    });
});
