/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 * Proprietary and confidential.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.peergreen.ipojo.management;

import org.apache.felix.ipojo.annotations.HandlerBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 *     {@code @MBean}
 *     {@link org.apache.felix.ipojo.annotations.Component @Component}
 *     public class ManagedComponent {
 *         ...
 *     }
 * </pre>
 * @see com.peergreen.ipojo.management.Description
 */
@HandlerBinding("com.peergreen.ipojo:management")
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MBean {

    /**
     * The object name to be used when registering the MBean.
     * Property substitution is possible:
     * <ul>
     *     <li>All component's properties</li>
     *     <li>iPOJO's own properties:
     *     <ul>
     *         <li>{@literal factory.name}</li>
     *         <li>{@literal component}</li>
     *         <li>{@literal instance.name}</li>
     *     </ul>
     *     </li>
     * </ul>
     */
    String value() default "ipojo:type=${factory.name},name=${instance.name}";
}
