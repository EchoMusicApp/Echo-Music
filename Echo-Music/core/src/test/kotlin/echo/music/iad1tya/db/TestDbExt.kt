package echo.music.iad1tya.db

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

fun SQLiteDatabase.toSupportSQLiteDatabase(): SupportSQLiteDatabase {
  val clazz = Class.forName("androidx.sqlite.db.framework.FrameworkSQLiteDatabase")
  val constructor = clazz.getDeclaredConstructor(SQLiteDatabase::class.java)
  constructor.isAccessible = true
  return constructor.newInstance(this) as SupportSQLiteDatabase
}
