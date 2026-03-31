(() => {
  const SPAIN = "ESP";
  const VALID_SPANISH_POSTAL_CODE = /^\d{5}$/;

  const forms = document.querySelectorAll("[data-address-form]");
  forms.forEach((form) => {
    const countryField = form.querySelector("[data-country-field]");
    const postalCodeField = form.querySelector("[data-postal-code-field]");
    const municipalityNameField = form.querySelector("[data-municipality-name-field]");
    const municipalityCodeField = form.querySelector("[data-municipality-code-field]");
    const municipalityTextShell = form.querySelector("[data-municipality-text-shell]");
    const spanishMunicipalityShell = form.querySelector("[data-spanish-municipality-shell]");
    const municipalitySelect = form.querySelector("[data-spanish-municipality-select]");
    const municipalityHint = form.querySelector("[data-spanish-municipality-hint]");

    if (!countryField || !postalCodeField || !municipalityNameField || !municipalityCodeField || !municipalityTextShell || !spanishMunicipalityShell || !municipalitySelect) {
      return;
    }

    let lastPostalLookup = "";

    const clearSpanishSelection = () => {
      municipalityCodeField.value = "";
      municipalityNameField.value = "";
      municipalitySelect.value = "";
    };

    const showManualMunicipality = () => {
      municipalityTextShell.hidden = false;
      spanishMunicipalityShell.hidden = true;
      municipalitySelect.innerHTML = "";
      municipalitySelect.disabled = true;
      municipalityCodeField.value = "";
      if (municipalityHint) {
        municipalityHint.hidden = true;
        municipalityHint.textContent = "";
      }
    };

    const showSpanishHint = (message) => {
      municipalityTextShell.hidden = true;
      spanishMunicipalityShell.hidden = false;
      municipalitySelect.innerHTML = "";
      municipalitySelect.disabled = true;

      const placeholder = document.createElement("option");
      placeholder.value = "";
      placeholder.textContent = "Municipio";
      municipalitySelect.appendChild(placeholder);

      clearSpanishSelection();

      if (municipalityHint) {
        municipalityHint.hidden = false;
        municipalityHint.textContent = message;
      }
    };

    const applySpanishOptions = (options) => {
      municipalityTextShell.hidden = true;
      spanishMunicipalityShell.hidden = false;
      municipalitySelect.disabled = false;
      municipalitySelect.innerHTML = "";

      const placeholder = document.createElement("option");
      placeholder.value = "";
      placeholder.textContent = options.length === 1 ? "Municipio seleccionado" : "Selecciona un municipio...";
      municipalitySelect.appendChild(placeholder);

      options.forEach((option) => {
        const optionElement = document.createElement("option");
        optionElement.value = option.municipalityCode;
        optionElement.textContent = option.provinceName
          ? `${option.municipalityName} · ${option.provinceName}`
          : option.municipalityName;
        optionElement.dataset.municipalityName = option.municipalityName;
        municipalitySelect.appendChild(optionElement);
      });

      if (options.length === 1) {
        municipalitySelect.value = options[0].municipalityCode;
        municipalityCodeField.value = options[0].municipalityCode;
        municipalityNameField.value = options[0].municipalityName;
      } else if (municipalityCodeField.value) {
        municipalitySelect.value = municipalityCodeField.value;
        if (municipalitySelect.value) {
          const selected = municipalitySelect.selectedOptions[0];
          municipalityNameField.value = selected?.dataset.municipalityName || "";
        } else {
          clearSpanishSelection();
        }
      } else {
        clearSpanishSelection();
      }

      if (municipalityHint) {
        municipalityHint.hidden = false;
        municipalityHint.textContent = options.length === 1
          ? "El municipio queda seleccionado automáticamente."
          : "Selecciona el municipio exacto para ese código postal.";
      }
    };

    const refreshMunicipalities = async () => {
      const country = countryField.value?.trim().toUpperCase() || "";
      const postalCode = postalCodeField.value?.trim() || "";

      if (country !== SPAIN) {
        showManualMunicipality();
        return;
      }

      municipalityTextShell.hidden = true;

      if (!VALID_SPANISH_POSTAL_CODE.test(postalCode)) {
        showSpanishHint("Escribe un código postal español de 5 números para ver los municipios disponibles.");
        return;
      }

      if (postalCode === lastPostalLookup) {
        return;
      }

      lastPostalLookup = postalCode;
      showSpanishHint("Cargando municipios...");

      try {
        const response = await fetch(`/municipality-catalog/spanish-municipalities?postalCode=${encodeURIComponent(postalCode)}`, {
          headers: {
            Accept: "application/json",
          },
        });
        if (!response.ok) {
          showSpanishHint("No se ha podido cargar el catálogo de municipios. Inténtalo de nuevo o revisa la configuración.");
          return;
        }
        const options = await response.json();
        if (!Array.isArray(options) || options.length === 0) {
          showSpanishHint("No hay municipios cargados para este código postal.");
          return;
        }
        applySpanishOptions(options);
      } catch (_error) {
        showSpanishHint("No se ha podido cargar el catálogo de municipios. Inténtalo de nuevo o revisa la configuración.");
      }
    };

    municipalitySelect.addEventListener("change", () => {
      const selected = municipalitySelect.selectedOptions[0];
      municipalityCodeField.value = municipalitySelect.value || "";
      municipalityNameField.value = selected?.dataset.municipalityName || "";
    });

    countryField.addEventListener("change", () => {
      lastPostalLookup = "";
      municipalityCodeField.value = "";
      municipalityNameField.value = "";
      refreshMunicipalities();
    });

    postalCodeField.addEventListener("input", () => {
      lastPostalLookup = "";
      clearSpanishSelection();
      refreshMunicipalities();
    });

    refreshMunicipalities();
  });
})();
