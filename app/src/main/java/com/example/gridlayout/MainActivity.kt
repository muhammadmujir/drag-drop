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
  private var gridLayoutWidth: Float = 0.0F
  private var gridLayoutHeight: Float = 0.0F

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(viewBinding.root)
    initView()
  }

  private fun initView(){
    with(viewBinding){
      setDragStarting(btnGrid, true)
      setDragListener()
      gridLayout.viewTreeObserver.addOnGlobalLayoutListener {
        gridLayoutWidth = gridLayout.width.toFloat()
        gridLayoutHeight = gridLayout.height.toFloat()
      }
      btnDelete.setOnClickListener {
        selectedTable?.let {
          Toast.makeText(this@MainActivity, "Delete Table:$it",Toast.LENGTH_LONG).show()
          tables.remove(it)
          (gridLayout[convertXY(it.x!!,it.y!!)] as Button).run {
            text = ""
            setOnClickListener(null)
            setOnLongClickListener(null)
            background = ContextCompat.getDrawable(this@MainActivity, android.R.color.white)
          }
          if (it.id!! < nextTableId!!){
            updateNextTableId(it.id)
          }
          selectedTable = null
        }
      }
      for (x in 0 until viewBinding.gridLayout.rowCount){
        for (y in 0 until viewBinding.gridLayout.columnCount){
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

  private fun setDragListener(){
    viewBinding.gridLayout.setOnDragListener { v, event ->
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
          val point = xyScreenToXyGrid(event.x, event.y)
          if ((viewBinding.gridLayout[convertXY(point.first, point.second)] as Button).text == ""){
            if (isNewTable){
              tables.add(Table(tableId?.toInt(), point.first, point.second))
              findNextTableId()
              updateNextTableId(nextTableId!!)
              (viewBinding.gridLayout[convertXY(point.first,point.second)] as Button).run {
                text = tableId
                background = ContextCompat.getDrawable(this@MainActivity, R.color.colorPrimary)
                onTableClicked(this, tables[tables.indexOfFirst { it.id == tableId?.toInt() }])
                setOnDragListener(null)
                setDragStarting(this, false)
              }
            } else {
              val prevTable = tables[tables.indexOfFirst { it.id == tableId?.toInt() }]
              (viewBinding.gridLayout[convertXY(prevTable.x!!, prevTable.y!!)] as Button).run {
                text = ""
                setOnClickListener(null)
                setOnLongClickListener(null)
                background = ContextCompat.getDrawable(this@MainActivity, android.R.color.white)
              }
              tables[tables.indexOfFirst { it.id == tableId?.toInt() }] = Table(tableId?.toInt(), point.first, point.second)
              (viewBinding.gridLayout[convertXY(point.first,point.second)] as Button).run {
                text = tableId
                onTableClicked(this, tables[tables.indexOfFirst { it.id == tableId?.toInt() }])
                setOnDragListener(null)
                setDragStarting(this, false)
                background = ContextCompat.getDrawable(this@MainActivity, R.color.colorPrimary)
              }
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
    return (x*viewBinding.gridLayout.columnCount) + y
  }

  private fun isTableAvailable(x: Int, y: Int): Pair<Boolean, Table?>{
    for (table in tables){
      if (table.x == x && table.y == y){
        return Pair(true, table)
      }
    }
    return Pair(false, null)
  }

  private fun xyScreenToXyGrid(width: Float, height: Float): Pair<Int, Int>{
    val itemWidth = gridLayoutWidth / viewBinding.gridLayout.columnCount.toFloat()
    val itemHeight = gridLayoutHeight / viewBinding.gridLayout.rowCount.toFloat()
    var colAxis: Int = (width/itemWidth).toInt() - 1
    var rowAxis: Int = (height/itemHeight).toInt() - 1
    if ((width/itemWidth) - colAxis > 0){
      colAxis++
    }
    if ((height/itemHeight) - rowAxis > 0){
      rowAxis++
    }
    return Pair(rowAxis, colAxis)
  }

}