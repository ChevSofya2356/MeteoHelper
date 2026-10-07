package com.example.meteohelper.ui.diary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meteohelper.R

class DiaryAdapter(
    private var entries: List<Map<String, Any>>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvEntryDate)
        val tvScore: TextView = itemView.findViewById(R.id.tvEntryScore)
        val tvSymptoms: TextView = itemView.findViewById(R.id.tvEntrySymptoms)
        val tvNotes: TextView = itemView.findViewById(R.id.tvEntryNotes)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteEntry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_entry, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val entry = entries[position]
        holder.tvDate.text = entry["date"] as? String ?: ""
        holder.tvScore.text = "Оценка: ${entry["score"]} / 10"
        holder.tvSymptoms.text = "Симптомы: ${entry["symptoms"]}"
        holder.tvNotes.text = entry["notes"] as? String ?: ""

        holder.btnDelete.setOnClickListener {
            val entryId = entry["id"] as Int
            onDeleteClick(entryId)
        }
    }

    override fun getItemCount() = entries.size

    fun updateEntries(newEntries: List<Map<String, Any>>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}