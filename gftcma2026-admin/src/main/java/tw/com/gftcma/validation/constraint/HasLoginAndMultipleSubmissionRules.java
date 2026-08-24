package tw.com.gftcma.validation.constraint;

import tw.com.gftcma.enums.CommonStatusEnum;

public interface HasLoginAndMultipleSubmissionRules {

	public CommonStatusEnum getRequireLogin();
	public CommonStatusEnum getAllowMultipleSubmissions();
	
}
