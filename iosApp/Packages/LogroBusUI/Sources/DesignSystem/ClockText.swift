import Foundation

/// Spec §5.7: a time as HH:mm on the Madrid clock, the same digits in every locale, as ETAs show clock times and the
/// planner its departure (LB-303).
public func clockText(hour: Int, minute: Int) -> String {
    String(format: "%02d:%02d", hour, minute)
}
