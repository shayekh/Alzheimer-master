package android.kaviles.bletutorial;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText phoneNumberET;
    private Button nextBtn;
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth=FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser()!=null){
            Intent intent=new Intent(LoginActivity.this,PlaceSelection.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }


        phoneNumberET=findViewById(R.id.phoneNumberET);
        nextBtn=findViewById(R.id.nextBtn);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber=phoneNumberET.getText().toString();

                if (!phoneNumber.equals("") && phoneNumber.matches("01[7,8,9,5,6]{1}[0-9]{8}")){
                    Intent intent=new Intent(LoginActivity.this,VerifyOtpActivity.class);
                    intent.putExtra("phoneNumber",phoneNumber);
                    startActivity(intent);

                }else {
                    Toast.makeText(LoginActivity.this, "Enter a valid phone number", Toast.LENGTH_LONG).show();
                }

            }
        });


    }
}
