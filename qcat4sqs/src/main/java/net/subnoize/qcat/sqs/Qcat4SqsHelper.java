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

package net.subnoize.qcat.sqs;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public class Qcat4SqsHelper {
	
	public static final Map<String,MessageAttributeValue> getAttributeMap() {
		return new HashMap<>();
	}

}