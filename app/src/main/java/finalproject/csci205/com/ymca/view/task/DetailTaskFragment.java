package finalproject.csci205.com.ymca.view.task;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import finalproject.csci205.com.ymca.R;
import finalproject.csci205.com.ymca.model.Subtask;
import finalproject.csci205.com.ymca.model.Task;
import finalproject.csci205.com.ymca.presenter.module.DetailTaskPresenter;
import finalproject.csci205.com.ymca.view.task.item.SimpleDividerItemDecoration;

/**
 * Created by ym012 on 11/16/2016.
 */
public class DetailTaskFragment extends Fragment implements View.OnKeyListener {

    public static final String SERIALIZED_TASK = "SERIALIZED_TASK";

    private DetailTaskPresenter detailTaskPresenter;
    private Task task;

    public DetailTaskFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_detail_task, container, false);
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        root.setOnKeyListener(this);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            task = (Task) bundle.getSerializable(SERIALIZED_TASK);
            getActivity().setTitle(task.getTitle());
            detailTaskPresenter = new DetailTaskPresenter(this, task);
        }

        initUI(root);
        initSubtaskList(root);

        return root;
    }

    private void initUI(View root) {
        final EditText editText = (EditText) root.findViewById(R.id.editText);

        Button addBtn = (Button) root.findViewById(R.id.addBtn);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Subtask newSubtask = new Subtask(task.getId(), editText.getText().toString());
                detailTaskPresenter.addSubtask(newSubtask);
            }
        });
    }

    private void initSubtaskList(View root) {
        RecyclerView rvSubtasks = (RecyclerView) root.findViewById(R.id.rvSubtasks);
        rvSubtasks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSubtasks.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
        rvSubtasks.setAdapter(detailTaskPresenter.getSubtasksAdapter());
    }


    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (i == KeyEvent.KEYCODE_BACK) {
            goToGTDFragment();
            return true;
        }

        return false;
    }

    private void goToGTDFragment() {
        Fragment fragment = GTDFragment.newInstance();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.content_nav, fragment).commit();
    }
}