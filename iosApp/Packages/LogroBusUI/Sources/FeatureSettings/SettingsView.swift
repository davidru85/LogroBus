import SwiftUI

/// Settings, opened from More (LB-012). Its preferences come with LB-403 (FR-SET-01): until then, its title alone.
public struct SettingsView: View {
    public init() {}

    public var body: some View {
        List {}
            .navigationTitle(Text("settings.title", bundle: .module))
    }
}

#Preview {
    NavigationStack {
        SettingsView()
    }
}
