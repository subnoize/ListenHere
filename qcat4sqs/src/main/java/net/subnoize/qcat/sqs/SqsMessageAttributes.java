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

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

/**
 * Helper class for SQS Attributes
 * 
 * @author youca
 *
 */
public class SqsMessageAttributes {
	
	private SqsMessageAttributes() {
		super();
	}
	
	public static final Builder builder() {
		return new Builder();
	}
	
	
	public static final class Builder {
		
		Map<String,MessageAttributeValue> attr = new HashMap<>();
		
		private Builder() {
			super();
		}
		
		public boolean hasAttr(String key) {
			return attr.containsKey(key);
		}
		
		public Builder attr(String key, String value) {
			attr.put(key, MessageAttributeValue.builder().dataType("String").stringValue(value).build());
			return this;
		}
		
		public Builder attr(String key, Number value) {
			attr.put(key, MessageAttributeValue.builder().dataType("Number").stringValue(value.toString()).build());
			return this;
		}
		
		public Builder attr(String key, byte[] value) {
			attr.put(key, MessageAttributeValue.builder().dataType("Binary").binaryValue(SdkBytes.fromByteArray(value)).build());
			return this;
		}

		public Builder attr(String key, Object value) {
			if(value instanceof String) {
				this.attr(key,(String)value);
			} else if(value instanceof Number) {
				this.attr(key,(Number)value);
			} else if(value instanceof byte[]) {
				this.attr(key,(byte)value);
			}
			return this;
		}
		
		public Map<String,MessageAttributeValue> build() {
			return attr;
		}
	}
}
