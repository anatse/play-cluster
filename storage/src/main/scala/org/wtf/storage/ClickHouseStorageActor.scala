package org.wtf.storage

import akka.actor.{Actor, ActorLogging}
import org.wtf.health.{HcFail, HcOk, HcResult, HealthCheck}
import org.wtf.storage.model.AuditEventEntity
import ru.yandex.clickhouse.BalancedClickhouseDataSource

import scala.util.{Failure, Success, Try}

/**
  * Class actor stores audit events in clickhouse database
  * @see docker image information: https://hub.docker.com/r/yandex/clickhouse-server/
  */
class ClickHouseStorageActor extends Actor with ActorLogging {
  private lazy val ds = new BalancedClickhouseDataSource("jdbc:clickhouse://localhost:8123/audit")
  private lazy val con = ds.getConnection

  // Creating audit table
  private val auditTable =
    """create table if not exists audit (
      | eventDate Date,
      | dateOccurred  DateTime,
      | ipAddress String,
      | userName String,
      | userIdentifier String,
      | channelName String,
      | subSystemName String,
      | eventGroupName String,
      | businessOperation String,
      | eventName String,
      | attributes Nested (
      |   attributeName String,
      |   attributeType String,
      |   value String
      | )
      |) ENGINE = MergeTree (eventDate, (channelName, subSystemName), 8192)
    """.stripMargin

  private val testInsert =
    """insert into audit (eventDate, dateOccurred, ipAddress, userName, userIdentifier, channelName, subSystemName,
      |eventGroupName, businessOperation, eventName, attributes.attributeName, attributes.attributeType, attributes.value)
      |values (now(), now(), 'ip', 'un', 'ui', 'cn', 'ssn', 'egn', 'bo', 'en', ['attr1'], ['string'], ['test value'])
    """.stripMargin

  private val insertSql = "insert into audit (" +
    "eventDate, " +
    "dateOccurred, " +
    "ipAddress, " +
    "userName, " +
    "userIdentifier, " +
    "channelName, " +
    "subSystemName, " +
    "eventGroupName, " +
    "businessOperation, " +
    "eventName, " +
    "attributes.attributeName," +
    "attributes.attributeType," +
    "attributes.value) " +
    "values (now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

  override def receive: Receive = {
    // Store audit entity
    case entity:AuditEventEntity => {
      sender ! storeAuditEntry(entity)
    }

    // HealthCheck operation
    case hc:HealthCheck => {
      val res:HcResult = Try (con.isValid(1)) match {
        case Success(true) => HcOk()
        case Success(false) => HcFail ("dead db")
        case Failure(msg) =>
          log.error(msg, "HealthCheck status:")
          HcFail(msg.getMessage)
      }

      sender ! res
    }
  }

  private def storeAuditEntry (entity:AuditEventEntity): HcResult = {
    Try {
      val stmt = con.prepareStatement(insertSql)
      stmt.closeOnCompletion()

      stmt.setTimestamp(1, entity.dateOccurred)
      stmt.setString(2, entity.ipAddress)
      stmt.setString(3, entity.userName)
      stmt.setString(4, entity.userIdentified)
      stmt.setString(5, entity.channelName)
      stmt.setString(6, entity.subSystemName)
      stmt.setString(7, entity.eventGroupName)
      stmt.setString(8, entity.businessOperation)
      stmt.setString(9, entity.eventName)
      stmt.setArray(10, con.createArrayOf("String", entity.attributes.map(_.attributeName).toArray))
      stmt.setArray(11, con.createArrayOf("String", entity.attributes.map(_.attributeType).toArray))
      stmt.setArray(12, con.createArrayOf("String", entity.attributes.map(_.value).toArray))

      stmt.execute()
      true

    } match {
      case Success(true) => HcOk()
      case Success(false) => HcFail("Unknown error")
      case Failure(ex) => HcFail(ex.getMessage)
    }
  }
}
