package uz.eslatma

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Item(val id: Long, val day: String, val min: Int, val text: String, var done: Boolean)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("eslatma", Context.MODE_PRIVATE)

    fun all(c: Context): MutableList<Item> {
        val a = JSONArray(sp(c).getString("items", "[]"))
        val l = mutableListOf<Item>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            l.add(Item(o.getLong("id"), o.getString("day"), o.getInt("min"), o.getString("text"), o.getBoolean("done")))
        }
        return l
    }

    fun save(c: Context, l: List<Item>) {
        val a = JSONArray()
        l.forEach {
            a.put(JSONObject().put("id", it.id).put("day", it.day).put("min", it.min).put("text", it.text).put("done", it.done))
        }
        sp(c).edit().putString("items", a.toString()).apply()
    }

    fun forDay(c: Context, day: String): List<Item> =
        all(c).filter { it.day == day }
            .sortedWith(compareBy<Item>({ if (it.min < 0) 99999 else it.min }, { it.id }))

    fun stage(c: Context, day: String) = sp(c).getInt("st_$day", 0)
    fun setStage(c: Context, day: String, s: Int) { sp(c).edit().putInt("st_$day", s).apply() }
}
