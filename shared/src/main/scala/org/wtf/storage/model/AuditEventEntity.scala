package org.wtf.storage.model

import java.sql.Timestamp

case class AuditEventAttr (
  attributeName: String,
  attributeType: String,
  mandatory: Boolean,

  // TODO consider to use more convenient approach with typification
  value: String
)

/*
 * TODO Consider to use Shapeless HList class to store parameters
 */
case class AuditEventEntity (
   dateOccurred: Timestamp,
   ipAddress: String,
   userName: String,
   userIdentified: String,
   channelName: String,
   subSystemName: String,
   eventGroupName: String,
   businessOperation: String,
   eventName: String,
   attributes: List[AuditEventAttr]
)

