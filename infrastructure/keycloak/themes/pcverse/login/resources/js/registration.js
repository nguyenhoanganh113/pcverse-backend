const form = document.getElementById("pcverse-registration-form");

if (form) {
    const localeButton = document.getElementById("kc-current-locale-link");
    const localeMenu = document.getElementById("language-switch1");
    const submitButton = document.getElementById("pcverse-register-button");
    const alert = document.getElementById("pcverse-registration-alert");
    const defaultSubmitLabel = submitButton.textContent.trim();

    const closeLocaleMenu = () => {
        if (!localeButton || !localeMenu) {
            return;
        }

        localeButton.setAttribute("aria-expanded", "false");
        localeMenu.style.removeProperty("display");
    };

    closeLocaleMenu();
    window.addEventListener("pageshow", closeLocaleMenu);

    const clearFeedback = () => {
        alert.hidden = true;
        alert.classList.remove("is-error", "is-success");
        alert.textContent = "";

        form.querySelectorAll("[id$='-error']").forEach((element) => {
            element.hidden = true;
            element.textContent = "";
        });

        form.querySelectorAll("[aria-invalid='true']").forEach((element) => {
            element.setAttribute("aria-invalid", "false");
        });
    };

    const showAlert = (message, type) => {
        alert.textContent = message;
        alert.classList.remove("is-error", "is-success");
        alert.classList.add(type === "success" ? "is-success" : "is-error");
        alert.hidden = false;
        alert.scrollIntoView({ behavior: "smooth", block: "center" });
    };

    const showFieldError = (field, message) => {
        const input = form.elements.namedItem(field);
        const error = document.getElementById(`${field}-error`);

        if (!input || !error) {
            return false;
        }

        input.setAttribute("aria-invalid", "true");
        error.textContent = message;
        error.hidden = false;
        return true;
    };

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearFeedback();

        if (!form.reportValidity()) {
            return;
        }

        const formData = new FormData(form);
        const password = String(formData.get("password") ?? "");
        const confirmPassword = String(formData.get("confirmPassword") ?? "");

        if (password !== confirmPassword) {
            showFieldError(
                "confirmPassword",
                form.dataset.passwordMismatch
            );
            form.elements.namedItem("confirmPassword").focus();
            return;
        }

        const avatar = String(formData.get("urlAvatar") ?? "").trim();
        const requestBody = {
            username: String(formData.get("username") ?? "").trim(),
            password,
            email: String(formData.get("email") ?? "").trim(),
            firstName: String(formData.get("firstName") ?? "").trim(),
            lastName: String(formData.get("lastName") ?? "").trim(),
            phoneNumber: String(formData.get("phoneNumber") ?? "").trim(),
            gender: String(formData.get("gender") ?? ""),
            dateOfBirth: String(formData.get("dateOfBirth") ?? ""),
            urlAvatar: avatar || null
        };

        submitButton.disabled = true;
        submitButton.textContent = form.dataset.submittingLabel;

        try {
            const response = await fetch(form.dataset.apiUrl, {
                method: "POST",
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(requestBody)
            });

            const responseBody = await response.json().catch(() => null);

            if (!response.ok) {
                const details = Array.isArray(responseBody?.details)
                    ? responseBody.details
                    : [];
                let hasFieldError = false;

                details.forEach((detail) => {
                    hasFieldError = showFieldError(
                        detail.field,
                        detail.message
                    ) || hasFieldError;
                });

                showAlert(
                    responseBody?.message
                        || (hasFieldError
                            ? "Please check the highlighted fields."
                            : "Registration failed. Please try again."),
                    "error"
                );
                return;
            }

            form.reset();
            showAlert(form.dataset.successMessage, "success");

            window.setTimeout(() => {
                window.location.assign(form.dataset.loginUrl);
            }, 1800);
        } catch (error) {
            showAlert(form.dataset.networkError, "error");
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = defaultSubmitLabel;
        }
    });
}
