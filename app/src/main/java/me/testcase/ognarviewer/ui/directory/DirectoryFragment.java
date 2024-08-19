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

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StableIdKeyProvider;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.testcase.ognarviewer.App;
import me.testcase.ognarviewer.MainActivity;
import me.testcase.ognarviewer.R;
import me.testcase.ognarviewer.directory.DirectoryEntry;

public class DirectoryFragment extends Fragment implements Toolbar.OnMenuItemClickListener,
        ActionMode.Callback, SearchView.OnQueryTextListener {
    private static final String TAG = "DirectoryFragment";

    private SelectionTracker<Long> mTracker;
    private DirectoryAdapter mAdapter;
    private ActionMode mActionMode;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::importJson);

    private final ActivityResultLauncher<String> mCreateDocument = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), this::exportJson);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_directory, container, false);

        // Set the adapter
        mAdapter = new DirectoryAdapter();
        final RecyclerView recyclerView = view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(mAdapter);

        mTracker = new SelectionTracker.Builder<Long>(
                "my-selection-id", // for save/restoreInstanceState
                recyclerView,
                new StableIdKeyProvider(recyclerView),
                new MyDetailsLookup(recyclerView),
                StorageStrategy.createLongStorage())
                .build();
        mTracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
            @Override
            public void onSelectionChanged() {
                if (mActionMode != null) {
                    if (mTracker.hasSelection()) {
                        mActionMode.setTitle(String.valueOf(mTracker.getSelection().size()));
                    } else {
                        mActionMode.finish();
                    }
                    return;
                }
                if (!mTracker.hasSelection()) {
                    return;
                }
                final MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
                mActionMode = toolbar.startActionMode(DirectoryFragment.this);
            }
        });

        mAdapter.setTracker(mTracker);

        mAdapter.setItems(App.getDirectoryRepository().list());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        enableActions();

        final NavController navController = Navigation.findNavController(view);
        final AppBarConfiguration appBarConfiguration =
                ((MainActivity) requireActivity()).getAppBarConfiguration();
        final MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this);
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

        final Menu menu = toolbar.getMenu();
        final MenuItem searchItem = menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        assert searchView != null;
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, String.format("DirectoryFragment %h destroys its view", this));
        mTracker = null;
        mAdapter = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        // Race condition: the fragment is destroyed before its View was created.
        if (mTracker != null) {
            mTracker.onSaveInstanceState(state);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle state) {
        super.onViewStateRestored(state);
        mTracker.onRestoreInstanceState(state);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_all_title)
                    .setMessage(R.string.delete_all_message)
                    .setNegativeButton(R.string.delete_all_cancel, null)
                    .setPositiveButton(R.string.delete_all_delete, (dialog, which) -> {
                        mAdapter.setItems(null);
                        //mAdapter.notifyItemRangeRemoved(0, mAdapter.getItemCount());
                        enableActions();
                        App.getDirectoryRepository().deleteAll();
                    })
                    .show();
        } else if (item.getItemId() == R.id.action_import) {
            mGetContent.launch("*/*");
        } else if (item.getItemId() == R.id.action_export) {
            mCreateDocument.launch("my-directory.json");
        }
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.fragment_directory_selection, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            final Selection<Long> selection = mTracker.getSelection();
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getResources().getQuantityString(
                            R.plurals.delete_count, selection.size(), selection.size()))
                    .setMessage(R.string.delete_message)
                    .setNegativeButton(R.string.delete_cancel, null)
                    .setPositiveButton(R.string.delete_delete, (dialog, which) -> {
                        final List<Long> idsToDelete = new ArrayList<>(selection.size());
                        final List<DirectoryEntry> entitiesToKeep =
                                new ArrayList<>(mAdapter.getItemCount() - selection.size());
                        for (int i = 0; i < mAdapter.getItemCount(); ++i) {
                            final DirectoryEntry entry = mAdapter.getItem(i);
                            if (selection.contains(entry.getId())) {
                                idsToDelete.add(entry.getId());
                            } else {
                                entitiesToKeep.add(entry);
                            }
                        }
                        App.getDirectoryRepository().delete(idsToDelete);
                        mActionMode.finish();
                        mAdapter.setItems(entitiesToKeep);
                        enableActions();
                    })
                    .show();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mTracker.clearSelection();
        mActionMode = null;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return onQueryTextChange(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mAdapter.setItems(App.getDirectoryRepository().filter(newText));
        return true;
    }

    private void importJson(@Nullable Uri uri) {
        if (uri == null) {
            // File dialog was canceled.
            return;
        }

        try {
            App.getDirectoryRepository().importJson(uri);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(requireActivity(), R.string.parse_error, Toast.LENGTH_LONG).show();
        }
    }

    private void exportJson(@Nullable Uri uri) {
        if (uri == null) {
            // File dialog was canceled.
            return;
        }

        try {
            App.getDirectoryRepository().exportJson(uri);
            Toast.makeText(requireActivity(), R.string.exported, Toast.LENGTH_LONG).show();
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void enableActions() {
        final boolean enabled = mAdapter.getItemCount() > 0;
        final MaterialToolbar toolbar = requireView().findViewById(R.id.toolbar);
        final Menu menu = toolbar.getMenu();
        final MenuItem exportItem = menu.findItem(R.id.action_export);
        if (exportItem != null) {
            exportItem.setEnabled(enabled);
        }
        final MenuItem deleteAllItem = menu.findItem(R.id.action_delete_all);
        if (deleteAllItem != null) {
            deleteAllItem.setEnabled(enabled);
        }

        final View emptyState = requireView().findViewById(R.id.empty_state);
        emptyState.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private static class MyDetailsLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView mRecyclerView;

        public MyDetailsLookup(RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            final View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                if (holder instanceof DirectoryAdapter.ViewHolder) {
                    return ((DirectoryAdapter.ViewHolder) holder).getItemDetails();
                }
            }
            return null;
        }
    }
}
