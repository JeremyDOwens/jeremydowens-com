/*
Core utilities to building JSON responses All responses will be return an object with
a top level object titled either "success" or "error" paired with a message that describes
the result. In practice, if additional information must be returned, it should be appended
the object using the + operator in a separate key-value pair.
 */

package com.jeremydowens.responseutils

import play.api.libs.json._

object JsonResponses {
  def error(message: String): JsObject = JsObject(Seq("error" -> JsString(message)))
  def success(message:String): JsObject = JsObject(Seq("success" -> JsString(message)))
}