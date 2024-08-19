/*
 * Copyright © 2024 Ivan Akulinchev <ivan.akulinchev@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.testcase.ognarviewer.ui.directory;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.testcase.ognarviewer.databinding.DirectoryEntryBinding;
import me.testcase.ognarviewer.directory.DirectoryEntry;
import me.testcase.ognarviewer.world.Target;

public class DirectoryAdapter extends RecyclerView.Adapter<DirectoryAdapter.ViewHolder> {
    private SelectionTracker<Long> mTracker;
    private List<DirectoryEntry> mItems;

    public DirectoryAdapter() {
        setHasStableIds(true);
    }

    public void setTracker(SelectionTracker<Long> tracker) {
        mTracker = tracker;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(DirectoryEntryBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final DirectoryEntry entry = mItems.get(position);
        holder.id = entry.getId();
        final int color = Target.COLORS[(int) (entry.getId() % Target.COLORS.length)];
        holder.indicatorView.setColorFilter(color);
        if (entry.getRegistration() != null) {
            holder.registrationView.setText(entry.getRegistration());
        } else {
            holder.registrationView.setText(entry.getCompetitionNumber());
        }
        holder.modelView.setText(entry.getModel());
        if (mTracker != null) {
            holder.itemView.setActivated(mTracker.isSelected(entry.getId()));
        }
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getId();
    }

    public void setItems(List<DirectoryEntry> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    public DirectoryEntry getItem(int position) {
        return mItems.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public long id;

        public final ImageView indicatorView;
        public final TextView registrationView;
        public final TextView modelView;

        private final ItemDetailsLookup.ItemDetails<Long> mItemDetails =
                new ItemDetailsLookup.ItemDetails<Long>() {
            @Override
            public int getPosition() {
                return getBindingAdapterPosition();
            }

            @NonNull
            @Override
            public Long getSelectionKey() {
                return getItemId();
            }
        };

        public ViewHolder(DirectoryEntryBinding binding) {
            super(binding.getRoot());
            indicatorView = binding.indicator;
            registrationView = binding.registration;
            modelView = binding.model;
            itemView.setOnClickListener(view -> {
                final NavController navController = Navigation.findNavController(view);
                navController.navigate(DirectoryFragmentDirections.editAction(id));
            });
        }

        ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return mItemDetails;
        }
    }
}
