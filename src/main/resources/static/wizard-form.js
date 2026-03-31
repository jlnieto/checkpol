document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("form").forEach((form) => {
        form.setAttribute("novalidate", "novalidate");
    });

    document.querySelectorAll("[data-wizard-form]").forEach((form) => {
        const panels = Array.from(form.querySelectorAll("[data-step-panel]"));
        const indicators = Array.from(form.querySelectorAll("[data-step-indicator]"));
        const progressLabel = form.querySelector("[data-step-progress]");
        const progressBar = form.querySelector("[data-step-progress-bar]");
        const stepTitle = form.querySelector("[data-current-step-title]");
        const stepHelp = form.querySelector("[data-current-step-help]");
        const wizardRoot = form.closest("[data-wizard-root]") || form.parentElement || form;
        const stepNoteShell = wizardRoot.querySelector("[data-step-note-shell]");
        const stepNote = wizardRoot.querySelector("[data-step-note-text]");
        if (panels.length === 0) {
            return;
        }

        setupGuestFormEnhancements(form);

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
                progressLabel.textContent = progressCopy(index, panels.length);
            }
            if (progressBar) {
                progressBar.style.width = `${((index + 1) / panels.length) * 100}%`;
            }

            const activePanel = panels[index];
            if (stepTitle && activePanel.dataset.stepTitle) {
                stepTitle.textContent = activePanel.dataset.stepTitle;
            }
            if (stepHelp && activePanel.dataset.stepHelp) {
                stepHelp.textContent = activePanel.dataset.stepHelp;
            }
            if (stepHelp) {
                const helpText = activePanel.dataset.stepHelp || "";
                stepHelp.textContent = helpText;
                stepHelp.hidden = helpText.trim() === "";
            }
            if (stepNoteShell && stepNote) {
                const noteText = activePanel.dataset.stepNote || "";
                stepNote.textContent = noteText;
                stepNoteShell.hidden = noteText.trim() === "";
            }

            syncGuestFormState(form);
            form.dataset.activeStep = String(index);
            scrollWizardToTop(form);
            focusFirstEditableField(activePanel);
        };

        const findFirstErrorStep = () => {
            const stepWithError = panels.findIndex((panel) => {
                if (panel.querySelector("[data-server-error]:not(:empty), [data-client-error-for]:not(:empty)")) {
                    return true;
                }

                return Array.from(panel.querySelectorAll("[data-panel-errors]"))
                    .some((box) => !box.hidden && box.textContent.trim() !== "");
            });

            if (stepWithError >= 0) {
                return stepWithError;
            }

            const requestedStep = Number(form.dataset.initialStep || "0");
            return Number.isNaN(requestedStep) ? 0 : requestedStep;
        };

        setActiveStep(findFirstErrorStep());

        form.querySelectorAll("[data-step-next]").forEach((button) => {
            button.addEventListener("click", () => {
                const currentStep = Number(form.dataset.activeStep || "0");
                const currentPanel = panels[currentStep];
                if (!validatePanel(form, currentPanel)) {
                    return;
                }
                setActiveStep(Math.min(currentStep + 1, panels.length - 1));
            });
        });

        form.querySelectorAll("[data-step-prev]").forEach((button) => {
            button.addEventListener("click", () => {
                const currentStep = Number(form.dataset.activeStep || "0");
                setActiveStep(Math.max(currentStep - 1, 0));
            });
        });

        form.addEventListener("submit", (event) => {
            if (event.submitter?.hasAttribute("data-skip-panel-validation")) {
                normalizeGuestFormValues(form);
                return;
            }

            normalizeGuestFormValues(form);
            const currentStep = Number(form.dataset.activeStep || `${panels.length - 1}`);
            const currentPanel = panels[currentStep];
            if (!validatePanel(form, currentPanel)) {
                event.preventDefault();
            }
        });
    });
});

function validatePanel(form, panel) {
    clearPanelErrors(panel);
    syncGuestFormState(form);

    const errors = form.hasAttribute("data-guest-form")
        ? validateGuestPanel(form, panel)
        : validateBookingPanel(form, panel);

    renderFieldErrors(panel, errors);
    if (errors.length === 0) {
        return true;
    }

    focusFirstFieldWithError(panel, errors[0].fieldName);
    return false;
}

function validateBookingPanel(form, panel) {
    const errors = [];
    const panelIndex = panelIndexFor(form, panel);

    if (panelIndex === 0) {
        const accommodationId = form.querySelector("[name='accommodationId']");
        const referenceCode = form.querySelector("[name='referenceCode']");
        const personCount = form.querySelector("[name='personCount']");

        requireValue(errors, accommodationId, "Selecciona una vivienda.");
        requireTrimmedValue(errors, referenceCode, "Escribe la referencia de Airbnb.");
        requireTrimmedValue(errors, personCount, "Indica cuantas personas vienen en la reserva.");

        if (personCount && personCount.value.trim() !== "") {
            const value = Number(personCount.value);
            if (Number.isNaN(value) || value < 1) {
                addFieldError(errors, "personCount", "Indica al menos 1 persona.");
            } else if (value > 20) {
                addFieldError(errors, "personCount", "El numero de personas no puede superar 20.");
            }
        }
    }

    if (panelIndex === 1) {
        const contractDate = form.querySelector("[name='contractDate']");
        const checkInDate = form.querySelector("[name='checkInDate']");
        const checkOutDate = form.querySelector("[name='checkOutDate']");

        requireValue(errors, contractDate, "Indica la fecha en la que cerraste la estancia.");
        requireValue(errors, checkInDate, "Indica la fecha de entrada.");
        requireValue(errors, checkOutDate, "Indica la fecha de salida.");

        const contract = parseDateValue(contractDate?.value);
        const checkIn = parseDateValue(checkInDate?.value);
        const checkOut = parseDateValue(checkOutDate?.value);

        if (checkIn && checkOut && compareDates(checkOut, checkIn) <= 0) {
            addFieldError(errors, "checkOutDate", "La fecha de salida debe ser posterior a la fecha de entrada.");
        }
        if (contract && checkIn && compareDates(contract, checkIn) > 0) {
            addFieldError(errors, "contractDate", "La fecha de reserva no puede ser posterior a la entrada.");
        }
    }

    return dedupeErrors(errors);
}

function validateGuestPanel(form, panel) {
    const errors = [];
    const panelIndex = panelIndexFor(form, panel);

    if (panelIndex === 0) {
        requireTrimmedValue(errors, form.querySelector("[name='firstName']"), "Escribe el nombre.");
        requireTrimmedValue(errors, form.querySelector("[name='lastName1']"), "Escribe el primer apellido.");
        requireValue(errors, form.querySelector("[name='birthDate']"), "Indica la fecha de nacimiento.");
        requireTrimmedValue(errors, form.querySelector("[name='nationality']"), "Selecciona la nacionalidad.");

        const birthDate = parseDateValue(form.querySelector("[name='birthDate']")?.value);
        if (birthDate) {
            const today = parseDateValue(todayIsoDate());
            const age = calculateAge(birthDate, today);
            if (compareDates(birthDate, today) >= 0 || age < 0) {
                addFieldError(errors, "birthDate", "La fecha de nacimiento no es valida.");
            } else if (age > 120) {
                addFieldError(errors, "birthDate", "Revisa la fecha de nacimiento.");
            }
        }

        const nationality = form.querySelector("[name='nationality']");
        if (nationality && nationality.value.trim() !== "" && !/^[A-Z]{3}$/.test(nationality.value.trim().toUpperCase())) {
            addFieldError(errors, "nationality", "Selecciona una nacionalidad valida.");
        }
    }

    if (panelIndex === 1) {
        const documentTypeField = form.querySelector("[name='documentType']");
        const documentNumberField = form.querySelector("[name='documentNumber']");
        const documentSupportField = form.querySelector("[name='documentSupport']");
        const relationshipField = form.querySelector("[name='relationship']");
        const requiresDocument = isDocumentRequired(form);
        const hasDocumentType = documentTypeField && documentTypeField.value !== "";
        const hasDocumentNumber = documentNumberField && documentNumberField.value.trim() !== "";
        const documentError = validateDocumentValue(documentTypeField?.value || "", documentNumberField?.value || "");

        if (requiresDocument && !hasDocumentType) {
            addFieldError(errors, "documentType", "Selecciona el tipo de documento.");
        }
        if (requiresDocument && !hasDocumentNumber) {
            addFieldError(errors, "documentNumber", "Escribe el numero del documento.");
        }
        if (hasDocumentType !== hasDocumentNumber) {
            if (hasDocumentType) {
                addFieldError(errors, "documentNumber", "Rellena tambien el numero del documento.");
            } else {
                addFieldError(errors, "documentType", "Selecciona tambien el tipo de documento.");
            }
        }
        if (documentError) {
            addFieldError(errors, "documentNumber", documentError);
        }
        if ((form.querySelector("[name='sex']")?.value || "") === "") {
            addFieldError(errors, "sex", "Selecciona el sexo.");
        }
        if (documentTypeField && documentSupportField && (documentTypeField.value === "NIF" || documentTypeField.value === "NIE")
            && documentSupportField.value.trim() === "") {
            addFieldError(errors, "documentSupport", "Indica el numero de soporte.");
        }
        if (isMinorAtCheckIn(form) && relationshipField && relationshipField.value === "") {
            addFieldError(errors, "relationship", "Selecciona el parentesco.");
        }
    }

    if (panelIndex === 2) {
        const selectedAddress = form.querySelector("input[name='addressId']:checked");
        if (!selectedAddress) {
            addFieldError(errors, "addressId", "Selecciona una direccion o crea una nueva.");
        }
    }

    if (panelIndex === 3) {
        const phoneField = form.querySelector("[name='phone']");
        const phone2Field = form.querySelector("[name='phone2']");
        const emailField = form.querySelector("[name='email']");
        const hasPhone = hasTrimmedValue(phoneField);
        const hasPhone2 = hasTrimmedValue(phone2Field);
        const hasEmail = hasTrimmedValue(emailField);

        if (!hasPhone && !hasPhone2 && !hasEmail) {
            addFieldError(errors, "phone", "Indica telefono o email.");
            addFieldError(errors, "phone2", "Indica telefono o email.");
            addFieldError(errors, "email", "Indica telefono o email.");
        }

        if (hasPhone && !isValidPhone(phoneField.value)) {
            addFieldError(errors, "phone", "Escribe un telefono valido.");
        }
        if (hasPhone2 && !isValidPhone(phone2Field.value)) {
            addFieldError(errors, "phone2", "Escribe un telefono valido.");
        }
        if (hasEmail && !isValidEmail(emailField.value)) {
            addFieldError(errors, "email", "Escribe un correo valido.");
        }
    }

    return dedupeErrors(errors);
}

function clearPanelErrors(panel) {
    panel.querySelectorAll("[data-client-error-for]").forEach((fieldError) => {
        fieldError.textContent = "";
    });

    panel.querySelectorAll("[data-field-status-for]").forEach((fieldStatus) => {
        fieldStatus.textContent = "";
    });

    panel.querySelectorAll("label, .field-shell").forEach((label) => {
        label.classList.remove("field-invalid", "field-valid");
    });

    panel.querySelectorAll("input, select").forEach((field) => {
        field.classList.remove("input-invalid", "input-valid");
    });

    const panelErrors = panel.querySelector("[data-panel-errors]");
    if (panelErrors) {
        panelErrors.hidden = true;
        panelErrors.innerHTML = "";
    }
}

function renderFieldErrors(panel, errors) {
    const errorsByField = new Map(errors.map((error) => [error.fieldName, error.message]));

    panel.querySelectorAll("[data-client-error-for]").forEach((fieldError) => {
        const fieldName = fieldError.dataset.clientErrorFor;
        fieldError.textContent = errorsByField.get(fieldName) || "";
    });

    panel.querySelectorAll("[data-field-status-for]").forEach((fieldStatus) => {
        const fieldName = fieldStatus.dataset.fieldStatusFor;
        if (errorsByField.has(fieldName)) {
            fieldStatus.textContent = "";
            return;
        }

        const field = fieldForErrorName(panel, fieldName);
        if (!field || !hasMeaningfulValue(field)) {
            fieldStatus.textContent = "";
            return;
        }

        fieldStatus.textContent = positiveStatusCopy(fieldName);
    });

    panel.querySelectorAll("input[name], select[name]").forEach((field) => {
        if (field.type === "hidden") {
            return;
        }

        const fieldName = field.dataset.errorField || field.name;
        const label = field.closest("label");
        const hasError = errorsByField.has(fieldName);
        const hasValue = hasMeaningfulValue(field);
        const eligibleForValidState = hasValue && !hasError && !field.disabled && !label?.hidden;

        field.classList.toggle("input-invalid", hasError);
        field.classList.toggle("input-valid", eligibleForValidState);
        if (hasError) {
            field.dataset.liveValidate = "true";
        } else {
            delete field.dataset.liveValidate;
        }
        if (label) {
            label.classList.toggle("field-invalid", hasError);
            label.classList.toggle("field-valid", eligibleForValidState);
        }
    });
}

function focusFirstFieldWithError(panel, fieldName) {
    const field = fieldForErrorName(panel, fieldName) || panel.querySelector(`[data-focus-for='${fieldName}']`);
    if (field) {
        field.scrollIntoView({ behavior: "smooth", block: "center" });
        window.setTimeout(() => field.focus(), 120);
    }
}

function fieldForErrorName(panel, fieldName) {
    return panel.querySelector(`[data-error-field='${fieldName}']`)
        || panel.querySelector(`[name='${fieldName}']`);
}

function focusFirstEditableField(panel) {
    const field = panel.querySelector("input:not([type='hidden']):not([disabled]), select:not([disabled]), button:not([disabled])");
    if (field && window.matchMedia("(min-width: 760px)").matches) {
        window.setTimeout(() => field.focus(), 60);
    }
}

function setupGuestFormEnhancements(form) {
    if (!form.hasAttribute("data-guest-form")) {
        return;
    }

    setupCountryAutocomplete(form);
    setupFieldNormalization(form);
    syncGuestFormState(form);
}

function setupCountryAutocomplete(form) {
    form.querySelectorAll("[data-country-display]").forEach((displayField) => {
        const hiddenFieldName = displayField.dataset.countryDisplay;
        const hiddenField = form.querySelector(`[name='${hiddenFieldName}']`);
        const options = Array.from(displayField.list?.options || []).map((option) => option.value);
        const codeToLabel = new Map(options.map((option) => [countryCodeFromLabel(option), option]));
        const labelToCode = new Map(options.map((option) => [normalizeCountryLabel(option), countryCodeFromLabel(option)]));

        displayField.dataset.errorField = hiddenFieldName;

        const syncDisplayFromCode = () => {
            if (!hiddenField || !hiddenField.value) {
                displayField.value = "";
                return;
            }
            displayField.value = codeToLabel.get(hiddenField.value.toUpperCase()) || hiddenField.value.toUpperCase();
        };

        const syncCodeFromDisplay = () => {
            if (!hiddenField) {
                return;
            }

            const rawValue = displayField.value.trim();
            if (rawValue === "") {
                hiddenField.value = "";
                return;
            }

            const normalizedLabel = normalizeCountryLabel(rawValue);
            const matchedCode = labelToCode.get(normalizedLabel);
            if (matchedCode) {
                hiddenField.value = matchedCode;
                displayField.value = codeToLabel.get(matchedCode) || rawValue;
                return;
            }

            if (/^[A-Za-z]{3}$/.test(rawValue)) {
                hiddenField.value = rawValue.toUpperCase();
            }
        };

        syncDisplayFromCode();

        displayField.addEventListener("input", () => {
            syncCodeFromDisplay();
            validateLiveField(form, displayField);
        });
        displayField.addEventListener("change", () => {
            syncCodeFromDisplay();
            validateLiveField(form, displayField);
        });
        displayField.addEventListener("blur", () => {
            syncCodeFromDisplay();
            syncDisplayFromCode();
            validateLiveField(form, displayField);
        });
    });
}

function setupFieldNormalization(form) {
    form.querySelectorAll("input, select").forEach((field) => {
        if (field.type === "hidden") {
            return;
        }

        field.addEventListener("input", () => {
            normalizeFieldValue(field);
            syncGuestFormState(form);
            if (shouldValidateOnInput(field)) {
                validateLiveField(form, field);
            }
        });

        field.addEventListener("change", () => {
            normalizeFieldValue(field);
            if (field.name === "documentType") {
                field.dataset.lockedByUser = field.value !== "" ? "true" : "";
            }
            syncGuestFormState(form);
            markFieldTouched(field);
            validateLiveField(form, field);
        });

        field.addEventListener("blur", () => {
            normalizeFieldValue(field, true);
            syncGuestFormState(form);
            markFieldTouched(field);
            validateLiveField(form, field);
        });
    });
}

function validateLiveField(form, field) {
    const panel = field.closest("[data-step-panel]");
    if (!panel || panel.hidden) {
        return;
    }

    const errors = validateGuestPanel(form, panel);
    applyFieldValidationState(panel, field, errors);
}

function syncGuestFormState(form) {
    if (!form.hasAttribute("data-guest-form")) {
        return;
    }

    const documentType = form.querySelector("[name='documentType']")?.value || "";
    const documentNumberField = form.querySelector("[name='documentNumber']");
    const documentSupportHelp = form.querySelector("[data-document-support-help]");
    const documentNumberHint = form.querySelector("[data-document-number-hint]");
    const documentSupportGroup = form.querySelector("[data-document-support-group]");
    const documentSupportField = form.querySelector("[name='documentSupport']");
    const documentTypeField = form.querySelector("[name='documentType']");
    const relationshipField = form.querySelector("[data-relationship-field]");
    const relationshipSelect = relationshipField?.querySelector("select");
    const minorAtCheckIn = isMinorAtCheckIn(form);
    const supportRequired = documentType === "NIF" || documentType === "NIE";

    if (documentTypeField && documentNumberField) {
        autoDetectDocumentType(documentTypeField, documentNumberField);
    }

    if (documentNumberField) {
        documentNumberField.placeholder = documentPlaceholder(documentType);
    }

    if (documentNumberHint) {
        documentNumberHint.textContent = documentHint(documentType);
    }

    if (documentSupportGroup) {
        documentSupportGroup.hidden = !supportRequired;
        if (!supportRequired && documentSupportField) {
            documentSupportField.value = "";
        }
    }

    if (documentSupportHelp) {
        documentSupportHelp.textContent = supportRequired
            ? "Numero que aparece en el documento"
            : "";
    }

    if (relationshipField && relationshipSelect) {
        relationshipField.hidden = !minorAtCheckIn;
        if (!minorAtCheckIn) {
            relationshipSelect.value = "";
        }
    }

    syncAddressSelectionState(form);
}

function syncAddressSelectionState(form) {
    form.querySelectorAll("[data-address-choice-card]").forEach((card) => {
        const input = card.querySelector(".address-choice-input");
        card.classList.toggle("address-choice-card-selected", Boolean(input?.checked));
    });
}

function normalizeGuestFormValues(form) {
    form.querySelectorAll("input, select").forEach((field) => normalizeFieldValue(field, true));
}

function normalizeFieldValue(field, aggressive = false) {
    if (!field || field.type === "hidden") {
        return;
    }

    if (field.matches("[data-auto-capitalize]")) {
        field.value = normalizePersonName(field.value, aggressive);
        return;
    }

    if (field.name === "documentNumber") {
        field.value = field.value.toUpperCase().replace(/\s+/g, "");
        return;
    }

    if (field.name === "documentSupport" || field.name === "postalCode") {
        field.value = field.value.toUpperCase().trim().replace(/\s+/g, aggressive ? "" : " ");
        return;
    }

    if (field.name === "phone" || field.name === "phone2") {
        field.value = formatPhoneValue(field.value);
        return;
    }

    if (field.name === "email") {
        field.value = field.value.trim().toLowerCase();
        return;
    }

    if (field.tagName === "INPUT" && field.type === "text") {
        field.value = collapseWhitespace(field.value, aggressive);
    }
}

function normalizePersonName(value, aggressive) {
    const collapsed = collapseWhitespace(value, aggressive);
    if (!aggressive) {
        return collapsed;
    }
    return collapsed
        .toLocaleLowerCase("es-ES")
        .replace(/(^|[\s'-])\p{L}/gu, (match) => match.toLocaleUpperCase("es-ES"));
}

function collapseWhitespace(value, aggressive) {
    const trimmed = aggressive ? value.trim() : value.replace(/^\s+/, "");
    return trimmed.replace(/\s{2,}/g, " ");
}

function formatPhoneValue(value) {
    const raw = value.trim();
    if (raw === "") {
        return "";
    }

    const compact = raw.replace(/[^\d+]/g, "");
    if (!compact.startsWith("+")) {
        const localDigits = compact.replace(/\D/g, "").slice(0, 9);
        return localDigits === "" ? "" : formatPhoneValue(`+34${localDigits}`);
    }

    if (compact.startsWith("+34")) {
        const digits = compact.slice(3).replace(/\D/g, "").slice(0, 9);
        if (digits.length === 0) {
            return "+34 ";
        }
        return `+34 ${digits.replace(/(\d{3})(\d{0,3})(\d{0,3}).*/, (_, a, b, c) => [a, b, c].filter(Boolean).join(" "))}`.trim();
    }

    return compact.replace(/(\+\d{1,3})(\d+)/, (_, prefix, rest) => `${prefix} ${rest}`);
}

function validateDocumentValue(documentType, documentNumber) {
    const value = documentNumber.trim().toUpperCase();
    if (value === "" || documentType === "") {
        return null;
    }

    if (documentType === "NIF") {
        if (!/^\d{8}[A-Z]$/.test(value)) {
            return "Introduce un DNI valido (8 numeros y letra)";
        }
        if (!hasValidSpanishLetter(value)) {
            return "La letra del DNI no coincide";
        }
    }

    if (documentType === "NIE") {
        if (!/^[XYZ]\d{7}[A-Z]$/.test(value)) {
            return "Introduce un NIE valido";
        }
        if (!hasValidSpanishLetter(value.replace("X", "0").replace("Y", "1").replace("Z", "2"))) {
            return "La letra del NIE no coincide";
        }
    }

    return null;
}

function hasValidSpanishLetter(documentNumber) {
    const letters = "TRWAGMYFPDXBNJZSQVHLCKE";
    const numericPart = Number(documentNumber.slice(0, -1));
    const expected = letters[numericPart % 23];
    return expected === documentNumber.slice(-1);
}

function documentPlaceholder(documentType) {
    if (documentType === "NIE") {
        return "X1234567L";
    }
    if (documentType === "PAS") {
        return "AA1234567";
    }
    if (documentType === "OTRO") {
        return "Numero del documento";
    }
    return "12345678Z";
}

function countryCodeFromLabel(label) {
    const match = label.match(/\(([A-Z]{3})\)\s*$/);
    return match ? match[1] : "";
}

function normalizeCountryLabel(value) {
    return value.trim().toLocaleLowerCase("es-ES");
}

function panelIndexFor(form, panel) {
    return Array.from(form.querySelectorAll("[data-step-panel]")).indexOf(panel);
}

function addFieldError(errors, fieldName, message) {
    errors.push({ fieldName, message });
}

function requireValue(errors, field, message) {
    if (field && field.value === "") {
        addFieldError(errors, field.name, message);
    }
}

function requireTrimmedValue(errors, field, message) {
    if (field && field.value.trim() === "") {
        addFieldError(errors, field.name, message);
    }
}

function hasTrimmedValue(field) {
    return !!field && field.value.trim() !== "";
}

function hasMeaningfulValue(field) {
    return field.type === "date"
        ? field.value !== ""
        : field.value.trim() !== "";
}

function isDocumentRequired(form) {
    const birthDate = parseDateValue(form.querySelector("[name='birthDate']")?.value);
    const checkInDate = parseDateValue(form.dataset.checkInDate || "");
    const nationality = (form.querySelector("[name='nationality']")?.value || "").toUpperCase();
    if (!birthDate || !checkInDate) {
        return false;
    }

    const age = calculateAge(birthDate, checkInDate);
    return age >= 18 || (age > 14 && nationality === "ESP");
}

function isMinorAtCheckIn(form) {
    const birthDate = parseDateValue(form.querySelector("[name='birthDate']")?.value);
    const checkInDate = parseDateValue(form.dataset.checkInDate || "");
    if (!birthDate || !checkInDate) {
        return false;
    }
    return calculateAge(birthDate, checkInDate) < 18;
}

function parseDateValue(value) {
    if (!value) {
        return null;
    }

    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (!match) {
        return null;
    }

    return {
        year: Number(match[1]),
        month: Number(match[2]),
        day: Number(match[3])
    };
}

function todayIsoDate() {
    const now = new Date();
    const year = now.getFullYear();
    const month = `${now.getMonth() + 1}`.padStart(2, "0");
    const day = `${now.getDate()}`.padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function calculateAge(birthDate, referenceDate) {
    let age = referenceDate.year - birthDate.year;
    if (
        referenceDate.month < birthDate.month
        || (referenceDate.month === birthDate.month && referenceDate.day < birthDate.day)
    ) {
        age -= 1;
    }
    return age;
}

function compareDates(left, right) {
    return dateToNumber(left) - dateToNumber(right);
}

function dateToNumber(value) {
    return (value.year * 10000) + (value.month * 100) + value.day;
}

function isValidPhone(value) {
    return /^\+\d[\d\s().-]{5,19}$/.test(value.trim());
}

function isValidEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

function normalizedValue(value) {
    return (value || "").trim().toUpperCase();
}

function dedupeErrors(errors) {
    const seen = new Set();
    return errors.filter((error) => {
        const key = `${error.fieldName}:${error.message}`;
        if (seen.has(key)) {
            return false;
        }
        seen.add(key);
        return true;
    });
}

function applyFieldValidationState(panel, field, errors) {
    const fieldName = field.dataset.errorField || field.name;
    const fieldError = errors.find((error) => error.fieldName === fieldName) || null;
    const fieldErrorNode = panel.querySelector(`[data-client-error-for='${fieldName}']`);
    const fieldStatusNode = panel.querySelector(`[data-field-status-for='${fieldName}']`);
    const label = field.closest("label");

    if (fieldErrorNode) {
        fieldErrorNode.textContent = fieldError ? fieldError.message : "";
    }

    if (fieldStatusNode) {
        fieldStatusNode.textContent = !fieldError && hasMeaningfulValue(field) ? positiveStatusCopy(fieldName) : "";
    }

    field.classList.toggle("input-invalid", !!fieldError);
    field.classList.toggle("input-valid", !fieldError && hasMeaningfulValue(field));
    if (label) {
        label.classList.toggle("field-invalid", !!fieldError);
        label.classList.toggle("field-valid", !fieldError && hasMeaningfulValue(field));
    }

    if (fieldError) {
        field.dataset.liveValidate = "true";
    } else if (hasMeaningfulValue(field)) {
        delete field.dataset.liveValidate;
    }
}

function shouldValidateOnInput(field) {
    return field.dataset.liveValidate === "true";
}

function markFieldTouched(field) {
    field.dataset.touched = "true";
}

function autoDetectDocumentType(documentTypeField, documentNumberField) {
    const value = documentNumberField.value.trim().toUpperCase();
    if (value === "" || documentTypeField.dataset.lockedByUser === "true") {
        return;
    }

    if (/^\d/.test(value)) {
        documentTypeField.value = "NIF";
    } else if (/^[XYZ]/.test(value)) {
        documentTypeField.value = "NIE";
    }
}

function positiveStatusCopy(fieldName) {
    if (fieldName === "documentNumber") {
        return "Documento valido";
    }
    return "Correcto";
}

function documentHint(documentType) {
    if (documentType === "NIE") {
        return "Empieza por X, Y o Z, sin espacios ni guiones";
    }
    if (documentType === "NIF") {
        return "8 numeros y una letra, sin espacios ni guiones";
    }
    return "Sin espacios ni guiones";
}

function progressCopy(index, panelCount) {
    return `Paso ${index + 1} de ${panelCount}`;
}

function scrollWizardToTop(form) {
    const anchor = form.querySelector(".wizard-header") || form;
    anchor.scrollIntoView({ behavior: "smooth", block: "start" });
}
