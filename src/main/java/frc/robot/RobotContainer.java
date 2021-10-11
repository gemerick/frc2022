package frc.robot;

import frc.robot.Subsystems.*;
import frc.robot.Subsystems.Swerve.*;

import edu.wpi.first.wpilibj.controller.PIDController;
import edu.wpi.first.wpilibj.controller.ProfiledPIDController;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.trajectory.TrajectoryConfig;
import edu.wpi.first.wpilibj.trajectory.TrajectoryGenerator;
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

import java.util.List;

/*
 * This class is where the bulk of the robot should be declared.  Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls).  Instead, the structure of the robot
 * (including subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems
  private final Drivetrain m_robotDrive = new Drivetrain();
  private final Shooter m_shooter = new Shooter();
  private final Turret m_turret = new Turret();

  private final GoalShoot m_goalShoot = new GoalShoot(m_shooter, m_turret, m_robotDrive);
  private final FeedShooter m_feedShoot = new FeedShooter(m_shooter, m_turret);
  private final FaceTurret m_faceTurret = new FaceTurret(m_turret, m_robotDrive);
  private final ShooterDefault m_shootDefault = new ShooterDefault(m_shooter);

  // The driver's controller
  XboxController m_driverController = new XboxController(OIConstants.kDriverControllerPort);
  XboxController m_operatorController = new XboxController(OIConstants.kOperatorControllerPort);

  private final DriveByController m_drive = new DriveByController(m_robotDrive, m_driverController);

  /**
   * The container for the robot. Contains subsystems, OI devices, and commands.
   */
  public RobotContainer() {
    // Configure the button bindings
    configureButtonBindings();

    // Configure default commands
    // Set the default drive command to split-stick arcade drive
    m_robotDrive.setDefaultCommand(m_drive);
    m_turret.setDefaultCommand(m_faceTurret);
    m_shooter.setDefaultCommand(m_shootDefault);

  }

  /**
   * Use this method to define your button->command mappings. Buttons can be
   * created by instantiating a {@link edu.wpi.first.wpilibj.GenericHID} or one of
   * its subclasses ({@link edu.wpi.first.wpilibj.Joystick} or
   * {@link XboxController}), and then calling passing it to a
   * {@link JoystickButton}.
   */
  private void configureButtonBindings() {
    // Reset drivetrain when down on the DPad is pressed
    new POVButton(m_driverController, 180).whenPressed(() -> m_robotDrive.reset(180.0));
    new POVButton(m_driverController, 0).whenPressed(() -> m_robotDrive.reset(0.0));

    // Spin up the shooter when the 'A' button is pressed
    new JoystickButton(m_driverController, Button.kA.value).whenPressed(m_goalShoot);

    // Turn off the shooter when the 'B' button is pressed
    new JoystickButton(m_driverController, Button.kB.value).whenPressed(() -> m_goalShoot.cancel());

    // Run the feeder when the 'X' button is held, but only if the shooter is at
    // speed and turret is aligned
    new JoystickButton(m_driverController, Button.kX.value).whileHeld(m_feedShoot)
        .whenReleased(() -> m_feedShoot.cancel());

    new JoystickButton(m_driverController, Button.kY.value).whileHeld(() -> m_shooter.reverseFeeder())
        .whenReleased(() -> m_shooter.stopFeeder());

    new JoystickButton(m_driverController, Button.kBumperRight.value).whenPressed(() -> m_drive.changeFieldOrient());

  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    // Create config for trajectory
    TrajectoryConfig config = new TrajectoryConfig(AutoConstants.kMaxSpeedMetersPerSecond,
        AutoConstants.kMaxAccelerationMetersPerSecondSquared)
            // Add kinematics to ensure max speed is actually obeyed
            .setKinematics(DriveConstants.kDriveKinematics);

    // An example trajectory to follow. All units in meters.
    Trajectory exampleTrajectory = TrajectoryGenerator.generateTrajectory(
        // Start at the origin facing the +X direction
        new Pose2d(0, 0, new Rotation2d(0.0)),
        // Pass through these two interior waypoints, making an 's' curve path
        List.of(new Translation2d(1.0, 1.0), new Translation2d(2.0, -1.0)),
        // End 3 meters straight ahead of where we started, facing forward
        new Pose2d(3.0, 0.0, new Rotation2d(0.0)), config);

    var thetaController = new ProfiledPIDController(AutoConstants.kPThetaController, 0, 0,
        AutoConstants.kThetaControllerConstraints);
    thetaController.enableContinuousInput(-Math.PI, Math.PI);

    SwerveControllerCommand swerveControllerCommand = new SwerveControllerCommand(exampleTrajectory,
        m_robotDrive::getPose, // Functional interface to feed supplier
        DriveConstants.kDriveKinematics,

        // Position controllers
        new PIDController(AutoConstants.kPXController, 0, 0), new PIDController(AutoConstants.kPYController, 0, 0),
        thetaController, m_robotDrive::setModuleStates, m_robotDrive);

    // Reset odometry to the starting pose of the trajectory.
    m_robotDrive.resetOdometry(exampleTrajectory.getInitialPose());

    // Run path following command, then stop at the end.
    return swerveControllerCommand.andThen(() -> m_robotDrive.drive(0, 0, 0, false))
        .andThen(() -> m_robotDrive.resetOdometry(new Pose2d(0, 0, new Rotation2d(0.0))));
  }

}