/**
* Copyright (c) 2000-present Liferay, Inc. All rights reserved.
*
* This library is free software; you can redistribute it and/or modify it under
* the terms of the GNU Lesser General Public License as published by the Free
* Software Foundation; either version 2.1 of the License, or (at your option)
* any later version.
*
* This library is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
* details.
*/
import UIKit


public class LiferayDDLFormLoadOperation: ServerOperation, NSCopying {

	public var structureId: Int64?
	public var userId: Int64?

	public var loadedRecord: DDLRecord?


	internal override var hudLoadingMessage: HUDMessage? {
		return ("Loading form...", details: "Wait few seconds...")
	}
	internal override var hudFailureMessage: HUDMessage? {
		return ("An error happened loading the form", details: nil)
	}

	internal var formData: DDLFormData {
		return screenlet.screenletView as DDLFormData
	}


	//MARK: ServerOperation

	override func validateData() -> Bool {
		if structureId == nil {
			return false
		}

		return true
	}

	override internal func doRun(#session: LRSession) {
		let service = LRDDMStructureService_v62(session: session)

		loadedRecord = nil

		let result = service.getStructureWithStructureId(structureId!, error: &lastError)

		if lastError == nil {
			if let xsd = result["xsd"]! as? String {
				if let userIdValue = result["userId"]! as? Int {
					userId = Int64(userIdValue)
				}

				loadedRecord = DDLRecord(xsd: xsd, locale: NSLocale.currentLocale())
			}
			else {
				lastError = createError(cause: .InvalidServerResponse)
			}
		}
	}


	//MARK: NSCopying

	public func copyWithZone(zone: NSZone) -> AnyObject {
		let result = LiferayDDLFormLoadOperation(screenlet: self.screenlet)

		result.onComplete = self.onComplete

		result.structureId = self.structureId
		result.userId = self.userId

		return result
	}


}
