/**
 * PoiNK Project : Apache POI for NetKernel.
 * Copyright 2008 by Tohono Consulting LLC. All rights reserved.
 * Copyright 2013 by Elephant Bird Consulting BVBA. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.elbeesee.poink.representation;

/**
 *
 * Representation Interface for HSSFCell.
 *  
 * @author Tom Hicks. 09/06/2008.
 * @author Tom Geudens. 2013/08/25.
 *
 */

import org.apache.poi.hssf.usermodel.HSSFCell;

public interface IHSSFCellRepresentation {
	public HSSFCell getCellReadOnly();
}
