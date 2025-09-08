package com.example.chat.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.chat.ModelManager;
import com.example.chat.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ModelSelectionFragment extends Fragment {

    private Spinner modelSpinner;
    private Button refreshButton;
    private List<String> models = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_model_selection, container, false);

        modelSpinner = view.findViewById(R.id.model_spinner);
        refreshButton = view.findViewById(R.id.refresh_button);

        refreshButton.setOnClickListener(v -> fetchModels());

        // 保存用户选择
        modelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < models.size()) {
                    ModelManager.setSelectedModel(models.get(position));
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        // 初始加载模型
        fetchModels();

        return view;
    }

    private void fetchModels() {
        new FetchModelsTask().execute();
    }

    private class FetchModelsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL("http://192.168.195.15:11434/api/tags");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    JSONArray modelsArray = jsonResponse.getJSONArray("models");
                    models.clear();

                    for (int i = 0; i < modelsArray.length(); i++) {
                        JSONObject model = modelsArray.getJSONObject(i);
                        models.add(model.getString("name"));
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            models
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    modelSpinner.setAdapter(adapter);

                    // 选择第一个可用模型，并保存到全局
                    if (!models.isEmpty()) {
                        modelSpinner.setSelection(0);
                        ModelManager.setSelectedModel(models.get(0));
                    }

                    Toast.makeText(requireContext(), "模型加载成功", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "解析模型数据失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "获取模型失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}