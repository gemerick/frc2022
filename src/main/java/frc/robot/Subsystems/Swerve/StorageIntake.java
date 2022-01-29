package frc.robot.Subsystems.Swerve;
import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Configrun;

public class StorageIntake extends SubsystemBase {
    private TalonSRX intakeMotor = new TalonSRX(Configrun.get(41,"storageIntakeID"));

    public void storageIntakeIn() {
        intakeMotor.set(ControlMode.PercentOutput,Configrun.get(0.2,"storageIntakeInPower"));
    }
    public void storageIntakeOut() {
        intakeMotor.set(ControlMode.PercentOutput,- Configrun.get(-0.2,"storageIntakeOutPower"));
    }
    public void storageIntakeStop() {
        intakeMotor.set(ControlMode.PercentOutput,0);
    }
}