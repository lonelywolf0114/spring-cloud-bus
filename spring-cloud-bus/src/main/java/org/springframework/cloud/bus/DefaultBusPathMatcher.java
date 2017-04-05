package org.springframework.cloud.bus;

import java.util.Comparator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * {@link BusPathMatcher} that matches application context ids with multiple, comma-separated, profiles.
 * Original https://gist.github.com/kelapure/61d3f948acf478cc95225ff1d7d239c4
 *
 * See https://github.com/spring-cloud/spring-cloud-config/issues/678
 *
 * @author Rohit Kelapure
 * @author Spencer Gibb
 */
public class DefaultBusPathMatcher implements PathMatcher {

	private static final Log log = LogFactory.getLog(DefaultBusPathMatcher.class);

	private final PathMatcher delagateMatcher;

	public DefaultBusPathMatcher(PathMatcher delagateMatcher) {
		this.delagateMatcher = delagateMatcher;
	}

	protected boolean matchMultiProfile(String pattern, String applicationContextID) {

		log.debug("matchMultiProfile : " + pattern + ", " + applicationContextID);

		// parse the application-context-id
		String[] appContextIDTokens = tokenizeToStringArray(applicationContextID,":");
		if (appContextIDTokens.length <= 1) {
			// no parts, default to delegate which already returned false;
			return false;
		}
		String selfProfiles = appContextIDTokens[1];

		// short circuit if possible
		String[] profiles = tokenizeToStringArray(selfProfiles,",");

		if (profiles.length == 1) {
			// there aren't multiple profiles to check, the delegate match was
			// originally false so return what delegate determined
			return false;
		}

		// gather candidate ids with a single profile rather than a comma separated list
		String[] idsWithSingleProfile = new String[profiles.length];

		for (int i = 0; i < profiles.length; i++) {
			//replace comma separated profiles with single profile
			String profile = profiles[i];
			String[] newTokens = new String[appContextIDTokens.length];
			System.arraycopy(appContextIDTokens, 0, newTokens, 0, appContextIDTokens.length);
			newTokens[1] = profile;
			idsWithSingleProfile[i] = StringUtils.arrayToDelimitedString(newTokens, ":");
		}

		for (String id : idsWithSingleProfile) {
			if (delagateMatcher.match(pattern, id)) {
				log.debug("matched true");
				return true;
			}
		}

		log.debug("matched false");
		return false;
	}

	@Override
	public boolean isPattern(String path) {
		return delagateMatcher.isPattern(path);
	}

	@Override
	public boolean match(String pattern, String path) {
		log.debug("In match: " + pattern + ", " + path);
		if (!delagateMatcher.match(pattern, path)) {
			return matchMultiProfile(pattern, path);
		}
		return true;
	}

	@Override
	public boolean matchStart(String pattern, String path) {
		return delagateMatcher.matchStart(pattern, path);
	}

	@Override
	public String extractPathWithinPattern(String pattern, String path) {
		return delagateMatcher.extractPathWithinPattern(pattern, path);
	}

	@Override
	public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
		return delagateMatcher.extractUriTemplateVariables(pattern, path);
	}

	@Override
	public Comparator<String> getPatternComparator(String path) {
		return delagateMatcher.getPatternComparator(path);
	}

	@Override
	public String combine(String pattern1, String pattern2) {
		return delagateMatcher.combine(pattern1, pattern2);
	}
}
