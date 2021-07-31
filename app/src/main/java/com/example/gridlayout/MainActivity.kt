package com.example.gridlayout

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.example.gridlayout.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
  private lateinit var viewBinding: ActivityMainBinding
  private val tables = mutableListOf(
    Table(1,0,0),
    Table(2,1,1),
    Table(3,2,0)
  )
  private var nextTableId: Int? = null
  private var selectedTable: Table? = null
  private var actionEnded: Boolean = true
  private var width: Int? = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)
    initView()
  }

  private fun initView(){
    with(viewBinding){
      setDragStarting(btnGrid, true)
      gridLayout.viewTreeObserver.addOnGlobalLayoutListener {
        width = gridLayout.width
      }
      btnDelete.setOnClickListener {
        Toast.makeText(this@MainActivity, "width : "+ width,Toast.LENGTH_LONG).show()
        selectedTable?.let {
          tables.remove(it)
          (gridLayout[convertXY(it.x!!,it.y!!)] as Button).run {
            text = ""
            setOnClickListener(null)
            setOnLongClickListener(null)
            this@MainActivity.setDragListener(this, it.x, it.y)
            background = ContextCompat.getDrawable(this@MainActivity, android.R.color.white)
          }
          if (it.id!! < nextTableId!!){
            updateNextTableId(it.id)
          }
          selectedTable = null
        }
      }
      for (x in 0..2){
        for (y in 0..1){
          val isTableAvailable = isTableAvailable(x,y)
          if (isTableAvailable.first){
            (gridLayout[convertXY(x,y)] as Button).run {
              if (nextTableId == null || nextTableId!! >= isTableAvailable.second?.id!!){
                nextTableId = isTableAvailable.second?.id?.plus(1)
              }
              updateNextTableId(nextTableId!!)
              text = isTableAvailable.second?.id.toString()
              onTableClicked(this, isTableAvailable.second!!)
              setDragStarting(this, false)
              background = ContextCompat.getDrawable(this@MainActivity, R.color.colorPrimary)
            }
          } else {
            (gridLayout[convertXY(x,y)] as Button).run {
              text = ""
              setDragListener(this, x, y)
              background = ContextCompat.getDrawable(this@MainActivity, android.R.color.white)
            }
          }
        }
      }
    }
  }

  private fun setDragStarting(button: Button, isNewTable: Boolean){
    button.apply {
      setOnLongClickListener { v: View ->
        val item = ClipData.Item(Intent()
          .putExtra("tableId", text)
          .putExtra("isNewTable", isNewTable)
        )
        val dragData = ClipData(
          "item_key",
          arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
          item)
        val myShadow = MyDragShadowBuilder(this)
        v.startDrag(
          dragData,   // the data to be dragged
          myShadow,   // the drag shadow builder
          null,       // no need to use local data
          0           // flags (not currently used, set to 0)
        )
      }
    }
  }

  private fun setDragListener(button: Button, x: Int, y: Int){
    button.setOnDragListener { v, event ->
      when (event.action) {
        DragEvent.ACTION_DRAG_STARTED -> {
          if (event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            v.invalidate()
            true
          } else {
            false
          }
        }
        DragEvent.ACTION_DRAG_ENTERED -> {
          v.invalidate()
          true
        }

        DragEvent.ACTION_DRAG_LOCATION -> {
          true
        }
        DragEvent.ACTION_DRAG_EXITED -> {
          v.invalidate()
          true
        }
        DragEvent.ACTION_DROP -> {
          val item: ClipData.Item = event.clipData.getItemAt(0)
          val dragIntent = item.intent
          val tableId = dragIntent.getStringExtra("tableId")
          val isNewTable = dragIntent.getBooleanExtra("isNewTable", false)
          if (isNewTable){
            tables.add(Table(dragIntent.getStringExtra("tableId")?.toInt(), x, y))
            findNextTableId()
            updateNextTableId(nextTableId!!)
            (viewBinding.gridLayout[convertXY(x,y)] as Button).run {
              text = tableId
              background = ContextCompat.getDrawable(this@MainActivity, R.color.colorPrimary)
              onTableClicked(this, tables[tables.size -1])
              setOnDragListener(null)
              setDragStarting(this, false)
            }
          } else {
            val prevTable = tables[tables.indexOfFirst { it.id == tableId?.toInt() }]
            (viewBinding.gridLayout[convertXY(prevTable.x!!, prevTable.y!!)] as Button).run {
              text = ""
              setOnClickListener(null)
              setOnLongClickListener(null)
              this@MainActivity.setDragListener(this, prevTable.x, prevTable.y)
              background = ContextCompat.getDrawable(this@MainActivity, android.R.color.white)
            }
            tables[tables.indexOfFirst { it.id == tableId?.toInt() }] = Table(tableId?.toInt(), x, y)
            (viewBinding.gridLayout[convertXY(x,y)] as Button).run {
              text = tableId
              onTableClicked(this, tables[tables.indexOfFirst { it.id == tableId?.toInt() }])
              setOnDragListener(null)
              setDragStarting(this, false)
              background = ContextCompat.getDrawable(this@MainActivity, R.color.colorPrimary)
            }
          }
//          Toast.makeText(this, "Dragged data is " + tableId +" " +tables.toString(), Toast.LENGTH_LONG).show()
          Toast.makeText(this, "position: "+event.x+" & "+ event.y, Toast.LENGTH_SHORT).show()
          v.invalidate()
          true
        }

        DragEvent.ACTION_DRAG_ENDED -> {
          v.invalidate()
          when (event.result) {
            true ->
              Toast.makeText(this, "The drop was handled.", Toast.LENGTH_SHORT)
            else ->
              Toast.makeText(this, "The drop didn't work.", Toast.LENGTH_SHORT)
          }.show()
          true
        }
        else -> {
          Log.e("DragDrop Example", "Unknown action type received by OnDragListener.")
          false
        }
      }
    }
  }

  private fun onTableClicked(button: Button, table: Table){
    button.run {
      setOnClickListener {
        selectedTable?.let {
          if (table.id!! != selectedTable?.id){
            (viewBinding.gridLayout[convertXY(it.x!!, it.y!!)] as Button).run {
              tag = "unselected"
              background = ContextCompat.getDrawable(this@MainActivity, R.color.colorPrimary)
            }
          }
        }
        if (tag == null || tag == "unselected"){
          tag = "selected"
          selectedTable = table
          background = ContextCompat.getDrawable(this@MainActivity, android.R.color.holo_orange_light)
        } else {
          tag = "unselected"
          selectedTable = null
          background = ContextCompat.getDrawable(this@MainActivity, R.color.colorPrimary)
        }
      }
    }
  }

  private fun updateNextTableId(id: Int){
    nextTableId = id
    viewBinding.btnGrid.text = id.toString()
    viewBinding.btnGrid.visibility = View.VISIBLE
  }

  private fun findNextTableId(){
    nextTableId = null
    tables.sortWith(compareBy({ it.id }))
    for (table in tables){
      if (nextTableId == null || nextTableId!! >= table.id!!){
        nextTableId = table.id?.plus(1)
      }
    }
  }

  private fun convertXY(x: Int, y: Int): Int{
    if (x == 0){
      return y
    }
    return (x*2) + y
  }

  private fun isTableAvailable(x: Int, y: Int): Pair<Boolean, Table?>{
    for (table in tables){
      if (table.x == x && table.y == y){
        return Pair(true, table)
      }
    }
    return Pair(false, null)
  }
}