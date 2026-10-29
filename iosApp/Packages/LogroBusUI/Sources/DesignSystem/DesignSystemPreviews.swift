import Shared
import SwiftUI

// Previews of the components in light and dark. The snapshot tests cover every case, size and text size.

private struct BadgesPreview: View {
    var body: some View {
        HStack(spacing: Tokens.Spacing.xSmall) {
            ForEach(LineBadgeSize.allCases, id: \.self) { size in
                LineBadge(code: "B1", color: 0xFFFD_0200, onColor: 0xFF00_0000, size: size)
            }
        }
        .padding()
    }
}

private struct EtasPreview: View {
    var body: some View {
        VStack(alignment: .trailing, spacing: Tokens.Spacing.small) {
            EtaText(eta: EtaUi(display: EtaDisplayNow.shared, lowConfidence: false, scheduled: false, delay: nil,
                               cancelled: false))
            EtaText(eta: EtaUi(display: EtaDisplayMinutes(minutes: 4), lowConfidence: true, scheduled: false,
                               delay: EtaDelayLate(minutes: 3), cancelled: false))
            EtaText(eta: EtaUi(display: EtaDisplayClockTime(hour: 9, minute: 5), lowConfidence: false,
                               scheduled: true, delay: nil, cancelled: false))
        }
        .padding()
    }
}

private struct GlassPreview: View {
    var body: some View {
        AdaptiveGlassContainer(spacing: Tokens.Spacing.medium) {
            HStack(spacing: Tokens.Spacing.medium) {
                Image(systemName: "location.fill")
                    .accessibilityLabel(Text(verbatim: "Recenter"))
                    .frame(width: Tokens.minTouchTarget, height: Tokens.minTouchTarget)
                    .adaptiveGlass(in: Circle())
                Button {} label: { Text(verbatim: "Buscar rutas") }
                    .buttonStyle(AdaptiveGlassProminentButtonStyle())
            }
        }
        .padding()
        .background(LinearGradient(colors: [.green, .cyan, .purple], startPoint: .leading, endPoint: .trailing))
    }
}

#Preview("Line badges") { BadgesPreview() }
#Preview("Line badges, dark") { BadgesPreview().preferredColorScheme(.dark) }
#Preview("ETAs") { EtasPreview() }
#Preview("ETAs, dark") { EtasPreview().preferredColorScheme(.dark) }
#Preview("Glass") { GlassPreview() }
#Preview("Glass, dark") { GlassPreview().preferredColorScheme(.dark) }
#Preview("Glass, iOS 18 fallback") { GlassPreview().environment(\.glassRendering, .fallback) }
#Preview("Glass, iOS 18 fallback, dark") {
    GlassPreview().environment(\.glassRendering, .fallback).preferredColorScheme(.dark)
}
