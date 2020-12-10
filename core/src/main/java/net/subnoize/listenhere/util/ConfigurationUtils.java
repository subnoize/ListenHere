/**
 * (c)opyright 2020 subnoize llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.subnoize.listenhere.util;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.NoArgsConstructor;

/**
 * A helper to unwrap and retrieve values from the Spring Configuration system.
 * 
 * @author John Bryant
 *
 */
@NoArgsConstructor
@Component
public class ConfigurationUtils {

	private static final String EMPTY_STRING = "";

	private static final Pattern pattern = Pattern.compile("[\\${}]");

	@Autowired
	private Environment env;

	/**
	 * Get a string from the Spring configuration sub-system, unwrapping the "${..}"
	 * in the process
	 * 
	 * @param key
	 * @return
	 */
	public String getString(String key) {
		return env.getProperty(pattern.matcher(key).replaceAll(EMPTY_STRING).trim());
	}

}
