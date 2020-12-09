package net.subnoize.listenhere.util;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@Component
public class ConfigurationUtils {

	private static final String EMPTY_STRING = "";

	private static final Pattern pattern = Pattern.compile("[\\${}]");

	@Autowired
	private Environment env;

	public String getString(String key) {
		return env.getProperty(pattern.matcher(key).replaceAll(EMPTY_STRING).trim());
	}

}
