/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright 
 * protection in the United States. Foreign copyrights may apply.
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
package gov.va.med.term.pendingConcept.mojo;

/**
 * {@link PendingConcept}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class PendingConcept 
{
	private long sctId_;
	private String description_;
	private long parentSctId_;
	private String parentDescription_;
	
	
	protected PendingConcept(long sctId, String description, long parentSCTId, String parentDescription)
	{
		sctId_ = sctId;
		description_ = description;
		parentSctId_ = parentSCTId;
		parentDescription_ = parentDescription;
	}
	
	/**
	 * @return the sCTID
	 */
	public long getSCTID() {
		return sctId_;
	}
	/**
	 * @return the description
	 */
	public String getFSN() {
		return description_;
	}
	/**
	 * @return the partentSCTID
	 */
	public long getParentSCTID() {
		return parentSctId_;
	}
	/**
	 * @return the parentDescription
	 */
	public String getParentDescription() {
		return parentDescription_;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PendingConcept [sctId_=" + sctId_ + ", description_="
				+ description_ + ", parentSctId_=" + parentSctId_
				+ ", parentDescription_=" + parentDescription_ + "]";
	}
	
	
}
