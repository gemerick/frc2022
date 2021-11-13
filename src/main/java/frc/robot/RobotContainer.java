package frc.robot;

import frc.robot.Subsystems.*;
import frc.robot.Subsystems.Swerve.*;

import edu.wpi.first.wpilibj.controller.PIDController;
import edu.wpi.first.wpilibj.controller.ProfiledPIDController;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.XboxController.*;
import edu.wpi.first.wpilibj2.command.*;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.POVButton;
import frc.robot.Commands.DriveByController;
import frc.robot.Commands.FaceTurret;
import frc.robot.Commands.FeedShooter;
import frc.robot.Commands.GoalShoot;
import frc.robot.Commands.ShooterDefault;
import frc.robot.Constants.*;


/*
 * This class is where the bulk of the robot should be declared.  Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls).  Instead, the structure of the robot
 * (including subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems
  private final Drivetrain m_robotDrive = new Drivetrain(); //Create Drivetrain Subsystem
  private final Shooter m_shooter = new Shooter(); //Create Shooter Subsystem
  private final Turret m_turret = new Turret(); //Create Turret Subsystem

  private final GoalShoot m_goalShoot = new GoalShoot(m_shooter, m_turret, m_robotDrive);   //Create GoalShoot Command
  private final FeedShooter m_feedShoot = new FeedShooter(m_shooter, m_turret);             //Create FeedShooter Command
  private final FaceTurret m_faceTurret = new FaceTurret(m_turret, m_robotDrive);           //Create FaceTurret Command
  private final ShooterDefault m_shootDefault = new ShooterDefault(m_shooter);              //Create ShooterDefault Command

  // The driver's controller
  XboxController m_driverController = new XboxController(OIConstants.kDriverControllerPort);
  XboxController m_operatorController = new XboxController(OIConstants.kOperatorControllerPort);

  private final DriveByController m_drive = new DriveByController(m_robotDrive, m_driverController);

  /**
   * The container for the robot. Contains subsystems, OI devices, and commands.
   */
  public RobotContainer() {
    configureButtonBindings(); // Configure the button bindings to commands using configureButtonBindings function

    // Configure default commands
    m_robotDrive.setDefaultCommand(m_drive); //Set drivetrain default command to "DriveByController" 
    m_turret.setDefaultCommand(m_faceTurret); //Set turret default command to "FaceTurret"
    m_shooter.setDefaultCommand(m_shootDefault); //Set shooter default command to "ShooterDefault"

  }

  /**
   * Use this method to define your button->command mappings. Buttons can be
   * created by instantiating a {@link edu.wpi.first.wpilibj.GenericHID} or one of
   * its subclasses ({@link edu.wpi.first.wpilibj.Joystick} or
   * {@link XboxController}), and then calling passing it to a
   * {@link JoystickButton}.
   */
  private void configureButtonBindings() {
    // Reset drivetrain when down/up on the DPad is pressed
    new POVButton(m_driverController, 180).whenPressed(() -> m_robotDrive.reset(180.0));
    new POVButton(m_driverController, 0).whenPressed(() -> m_robotDrive.reset(0.0));

    // Run "GoalShoot" command when A is pressed on the joystick
    new JoystickButton(m_driverController, Button.kA.value).whenPressed(m_goalShoot);

    // Cancel "GoalShoot" command when A is pressed on the joystick
    new JoystickButton(m_driverController, Button.kB.value).whenPressed(() -> m_goalShoot.cancel());

    // Run "FeedShooter" command when X is held down and canel it when button is released
    new JoystickButton(m_driverController, Button.kX.value).whileHeld(m_feedShoot)
        .whenReleased(() -> m_feedShoot.cancel());

    // Call the reverseFeeder funciton from the shooter subclass when the Y button is held and stop the feeder when the button is released
    new JoystickButton(m_driverController, Button.kY.value).whileHeld(() -> m_shooter.reverseFeeder())
        .whenReleased(() -> m_shooter.stopFeeder());

    // Call the changeFieldOrient function when the Right Bumper is pressed
    new JoystickButton(m_driverController, Button.kBumperRight.value).whenPressed(() -> m_drive.changeFieldOrient());

  }

  /**
   * FYI AUTONOMOUS CODE IS WIP AND PROOF OF CONCEPT
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand(Trajectory trajectory) {
    
    var thetaController = new ProfiledPIDController(AutoConstants.kPThetaController, 0, 0,
        AutoConstants.kThetaControllerConstraints);
    thetaController.enableContinuousInput(-Math.PI, Math.PI);

    SwerveControllerCommand swerveControllerCommand = new SwerveControllerCommand(trajectory,
        m_robotDrive::getPose, // Functional interface to feed supplier
        DriveConstants.kDriveKinematics,

        // Position controllers
        new PIDController(AutoConstants.kPXController, 0, 0), new PIDController(AutoConstants.kPYController, 0, 0),
        thetaController, m_robotDrive::setModuleStates, m_robotDrive);

    // Reset odometry to the starting pose of the trajectory.
    m_robotDrive.resetOdometry(trajectory.getInitialPose());

    // Run path following command, then stop at the end.
    return swerveControllerCommand.andThen(() -> m_robotDrive.drive(0, 0, 0, false))
        .andThen(() -> m_robotDrive.resetOdometry(new Pose2d(0, 0, new Rotation2d(0.0))));
  }

}