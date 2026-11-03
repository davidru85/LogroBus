import Shared
import SwiftUI

/// Settings, opened from More (LB-012, LB-403).
public struct SettingsView: View {
    @State private var viewModel: SettingsViewModel

    public init(viewModel: SettingsViewModel = SettingsViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        List {}
            .navigationTitle(Text("settings.title", bundle: .module))
    }
}

#Preview {
    NavigationStack {
        SettingsView(viewModel: SettingsViewModel(useCases: SharedDependencies.shared.preview(), language: .spanish))
    }
}
