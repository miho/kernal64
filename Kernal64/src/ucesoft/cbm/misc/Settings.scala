package ucesoft.cbm.misc

import java.util.Properties

object Settings {
  trait SettingConv[T] {
    def convert(value:String) : T
    val defValue : T
    val consume : Int
  }
  
  implicit object StringSettingConv extends SettingConv[String] {
    def convert(value:String) = value
    val defValue = "" 
    val consume = 1
  }
  implicit object IntSettingConv extends SettingConv[Int] {
    def convert(value:String) = value.toInt
    val defValue = 0
    val consume = 1
  }
  implicit object doubleSettingConv extends SettingConv[Double] {
    def convert(value:String) = value.toDouble
    val defValue = 0
    val consume = 1
  }
  implicit object BooleanSettingConv extends SettingConv[Boolean] {
    def convert(value:String) = value.toBoolean
    val defValue = false
    val consume = 0
  }
}

class Settings {
  import Settings._
  
  private class Setting[T](val cmdLine : String,
                          val description : String,
                          key : String,
                          loadF : T => Unit,
                          saveF : => T)(implicit settingConv:SettingConv[T]) {
    def load(p:Properties) {
      val value = p.getProperty(key) match {
        case null => settingConv.defValue
        case v => settingConv.convert(v)
      }
      loadF(value)
    }
    
    def load(value:String) {
      loadF(settingConv.convert(value))
    }
    
    def save(p:Properties) {
      if (saveF != null) p.setProperty(key,saveF.toString)
    }
    
    def consume : Int = settingConv.consume
  }
  
  private[this] val settings = new collection.mutable.ListBuffer[Setting[_]]
  
  def add[T](cmdLine : String,
            description : String,
            key : String,
            loadF : T => Unit,
            saveF : => T)(implicit settingConv:SettingConv[T]) {
    settings += new Setting(cmdLine,description,key,loadF,saveF)
  } 
  
  def add[T](cmdLine : String,
            description : String,
            loadF : T => Unit)(implicit settingConv:SettingConv[T]) {
    settings += new Setting(cmdLine,description,"",loadF,null.asInstanceOf[T])
  } 
  
  def load(p:Properties) {
    for(s <- settings) s.load(p)
  }
  
  def save(p:Properties) {
    for(s <- settings) s.save(p)
  }
  
  def checkForHelp(args:Array[String]) : Boolean = args.length == 1 && (args(0) == "--help" || args(0) == "-h" || args(0) == "-help")
  
  def printUsage {
    println("Usage: [settings] [file to attach]")
    for(s <- settings) {
      val opt = if (s.cmdLine.length > 20) s.cmdLine else s.cmdLine + (" " * (20 - s.cmdLine.length))
      println("--" + opt + s.description)
    }
  }
  
  def parseAndLoad(args:Array[String]) : Option[String] = {
    var p = 0
    while (p < args.length && args(p).startsWith("--")) {
      settings find { _.cmdLine == args(p).substring(2) } match {
        case Some(s) if s.consume == 0 =>
          if (p + 1 < args.length && !args(p + 1).startsWith("--")) {
            s.load(args(p + 1))
            p += 2
          }
          else {
            s.load("true")
            p += 1
          }
        case Some(s) if s.consume == 1 =>
          if (p + 1 < args.length) s.load(args(p + 1))
          else throw new IllegalArgumentException("Value for setting " + args(p) + " not found")
          p += 2
        case None =>
          throw new IllegalArgumentException("Invalid setting: " + args(p))
      }
    }
    if (p < args.length) Some(args(p)) else None
  }
}