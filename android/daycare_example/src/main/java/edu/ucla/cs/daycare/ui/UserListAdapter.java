package edu.ucla.cs.daycare.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import edu.ucla.cs.daycare.R;
import edu.ucla.cs.daycare.model.User;

/**
 * Created by ethan on 5/25/16.
 */
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private ArrayList<User> mUserList;

    /**
     * Construct adapter.
     * @param userList The list of users to display.
     */
    public UserListAdapter(List<User> userList) {
        mUserList = new ArrayList<>(userList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.user_list_adapter, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.populate(mUserList.get(position));
    }

    @Override
    public int getItemCount() {
        return mUserList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private EditText mFirstName;
        private EditText mLastName;

        public ViewHolder(View view) {
            super(view);
            mFirstName = (EditText) view.findViewById(R.id.user_list_adapter_first_name);
            mLastName = (EditText) view.findViewById(R.id.user_list_adapter_last_name);
        }

        public void populate(final User user) {
            mFirstName.setText(user.firstName());
            mLastName.setText(user.lastName());
        }

    }
}
