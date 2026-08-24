package tw.com.gftcma.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tw.com.gftcma.enums.CommonStatusEnum;
import tw.com.gftcma.validation.annotation.ValidRequireLoginIfMultipleSubmissions;
import tw.com.gftcma.validation.constraint.HasLoginAndMultipleSubmissionRules;

public class RequireLoginIfMultipleSubmissionsValidator
		implements ConstraintValidator<ValidRequireLoginIfMultipleSubmissions, HasLoginAndMultipleSubmissionRules> { // Use AddFormDTO or a common interface if both DTOs share it

	@Override
	public boolean isValid(HasLoginAndMultipleSubmissionRules dto, ConstraintValidatorContext context) {
		if (dto == null) {
			return true; // Null DTOs are valid by default; handle elsewhere if needed
		}

		CommonStatusEnum allowMultiple = dto.getAllowMultipleSubmissions();
		CommonStatusEnum requireLogin = dto.getRequireLogin();

		// If allowMultipleSubmissions is true (ONE), requireLogin must also be true (ONE)
		if (allowMultiple == CommonStatusEnum.YES && requireLogin != CommonStatusEnum.YES) {
			return false;
		}

		return true;
	}
}