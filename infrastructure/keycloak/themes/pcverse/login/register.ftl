<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayRequiredFields=false; section>
    <#if section = "header">
        ${msg("registerTitle")}
    <#elseif section = "form">
        <div
            id="pcverse-registration-alert"
            class="pcverse-registration-alert"
            role="status"
            aria-live="polite"
            hidden
        ></div>

        <form
            id="pcverse-registration-form"
            class="${properties.kcFormClass!} pcverse-registration-form"
            action="${properties.registrationApiUrl!}"
            method="post"
            data-api-url="${properties.registrationApiUrl!}"
            data-login-url="${url.loginUrl}"
            data-password-mismatch="${msg("pcversePasswordMismatch")}"
            data-network-error="${msg("pcverseRegistrationNetworkError")}"
            data-success-message="${msg("pcverseRegistrationSuccess")}"
            data-submitting-label="${msg("pcverseRegistering")}"
        >
            <div class="pcverse-registration-intro">
                <p>${msg("pcverseRegistrationIntro")}</p>
                <span><span class="required">*</span> ${msg("pcverseRequiredFields")}</span>
            </div>

            <div class="pcverse-form-grid">
                <div class="${properties.kcFormGroupClass!}">
                    <label for="username" class="${properties.kcLabelClass!}">
                        ${msg("username")} <span class="required">*</span>
                    </label>
                    <input
                        id="username"
                        name="username"
                        type="text"
                        class="${properties.kcInputClass!}"
                        autocomplete="username"
                        maxlength="100"
                        required
                        autofocus
                        aria-describedby="username-error"
                    />
                    <span id="username-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="email" class="${properties.kcLabelClass!}">
                        ${msg("email")} <span class="required">*</span>
                    </label>
                    <input
                        id="email"
                        name="email"
                        type="email"
                        class="${properties.kcInputClass!}"
                        autocomplete="email"
                        maxlength="254"
                        required
                        aria-describedby="email-error"
                    />
                    <span id="email-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="firstName" class="${properties.kcLabelClass!}">
                        ${msg("firstName")} <span class="required">*</span>
                    </label>
                    <input
                        id="firstName"
                        name="firstName"
                        type="text"
                        class="${properties.kcInputClass!}"
                        autocomplete="given-name"
                        maxlength="100"
                        required
                        aria-describedby="firstName-error"
                    />
                    <span id="firstName-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="lastName" class="${properties.kcLabelClass!}">
                        ${msg("lastName")} <span class="required">*</span>
                    </label>
                    <input
                        id="lastName"
                        name="lastName"
                        type="text"
                        class="${properties.kcInputClass!}"
                        autocomplete="family-name"
                        maxlength="100"
                        required
                        aria-describedby="lastName-error"
                    />
                    <span id="lastName-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="phoneNumber" class="${properties.kcLabelClass!}">
                        ${msg("pcversePhoneNumber")} <span class="required">*</span>
                    </label>
                    <input
                        id="phoneNumber"
                        name="phoneNumber"
                        type="tel"
                        class="${properties.kcInputClass!}"
                        autocomplete="tel"
                        maxlength="20"
                        required
                        aria-describedby="phoneNumber-error"
                    />
                    <span id="phoneNumber-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="gender" class="${properties.kcLabelClass!}">
                        ${msg("pcverseGender")} <span class="required">*</span>
                    </label>
                    <select
                        id="gender"
                        name="gender"
                        class="${properties.kcInputClass!}"
                        required
                        aria-describedby="gender-error"
                    >
                        <option value="" selected disabled>${msg("pcverseSelectGender")}</option>
                        <option value="MALE">${msg("pcverseGenderMale")}</option>
                        <option value="FEMALE">${msg("pcverseGenderFemale")}</option>
                        <option value="OTHER">${msg("pcverseGenderOther")}</option>
                    </select>
                    <span id="gender-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="dateOfBirth" class="${properties.kcLabelClass!}">
                        ${msg("pcverseDateOfBirth")} <span class="required">*</span>
                    </label>
                    <input
                        id="dateOfBirth"
                        name="dateOfBirth"
                        type="date"
                        class="${properties.kcInputClass!}"
                        autocomplete="bday"
                        required
                        aria-describedby="dateOfBirth-error"
                    />
                    <span id="dateOfBirth-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="urlAvatar" class="${properties.kcLabelClass!}">
                        ${msg("pcverseAvatarUrl")}
                    </label>
                    <input
                        id="urlAvatar"
                        name="urlAvatar"
                        type="url"
                        class="${properties.kcInputClass!}"
                        autocomplete="url"
                        placeholder="https://"
                        maxlength="2048"
                        aria-describedby="urlAvatar-error"
                    />
                    <span id="urlAvatar-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="password" class="${properties.kcLabelClass!}">
                        ${msg("password")} <span class="required">*</span>
                    </label>
                    <div class="${properties.kcInputGroup!}" dir="ltr">
                        <input
                            id="password"
                            name="password"
                            type="password"
                            class="${properties.kcInputClass!}"
                            autocomplete="new-password"
                            minlength="8"
                            required
                            aria-describedby="password-error"
                        />
                        <button
                            class="${properties.kcFormPasswordVisibilityButtonClass!}"
                            type="button"
                            aria-label="${msg("showPassword")}"
                            aria-controls="password"
                            data-password-toggle
                            data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}"
                            data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                            data-label-show="${msg("showPassword")}"
                            data-label-hide="${msg("hidePassword")}"
                        >
                            <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                        </button>
                    </div>
                    <span id="password-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="confirmPassword" class="${properties.kcLabelClass!}">
                        ${msg("passwordConfirm")} <span class="required">*</span>
                    </label>
                    <div class="${properties.kcInputGroup!}" dir="ltr">
                        <input
                            id="confirmPassword"
                            name="confirmPassword"
                            type="password"
                            class="${properties.kcInputClass!}"
                            autocomplete="new-password"
                            minlength="8"
                            required
                            aria-describedby="confirmPassword-error"
                        />
                        <button
                            class="${properties.kcFormPasswordVisibilityButtonClass!}"
                            type="button"
                            aria-label="${msg("showPassword")}"
                            aria-controls="confirmPassword"
                            data-password-toggle
                            data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}"
                            data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}"
                            data-label-show="${msg("showPassword")}"
                            data-label-hide="${msg("hidePassword")}"
                        >
                            <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
                        </button>
                    </div>
                    <span id="confirmPassword-error" class="${properties.kcInputErrorMessageClass!}" hidden></span>
                </div>
            </div>

            <div class="pcverse-registration-actions">
                <a href="${url.loginUrl}">${msg("backToLogin")}</a>
                <button
                    id="pcverse-register-button"
                    class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                    type="submit"
                >
                    ${msg("doRegister")}
                </button>
            </div>
        </form>

        <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
        <script type="module" src="${url.resourcesPath}/js/registration.js"></script>
    </#if>
</@layout.registrationLayout>
