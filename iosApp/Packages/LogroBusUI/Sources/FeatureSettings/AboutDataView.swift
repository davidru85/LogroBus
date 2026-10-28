import Shared
import SwiftUI

/// About data (LB-404): so far, the last successful sync of each dataset (FR-SET-03). More opens it once the app shell
/// lands (LB-012); the data source, disclaimer and attributions come with LB-404.
public struct AboutDataView: View {
    @State private var viewModel: AboutDataViewModel

    public init(viewModel: AboutDataViewModel = AboutDataViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        content
            // Cancelled when the screen leaves, which stops the use case's collection.
            .task { await viewModel.observe() }
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: viewModel.state) {
        case .loading:
            ProgressView()
        case .content(let content):
            List {
                Section {
                    ForEach(content.lastSyncs, id: \.dataset) { sync in
                        LabeledContent {
                            if let lastSuccess = sync.lastSuccess {
                                Text(lastSuccess.formatted)
                            } else {
                                Text("about.last_sync.never", bundle: .module)
                            }
                        } label: {
                            Text(sync.dataset.label, bundle: .module)
                        }
                    }
                } header: {
                    Text("about.last_sync.title", bundle: .module)
                }
            }
        case .error:
            EmptyView()
        }
    }
}

extension StaticDataset {
    /// The dataset's name (design_guidelines.md §9, keys `about.dataset.*`).
    var label: LocalizedStringKey {
        switch self {
        case .lines: "about.dataset.lines"
        case .stops: "about.dataset.stops"
        }
    }
}

extension MadridDateTime {
    /// The time in the user's locale, as a medium date and a short time (AGENTS.md §5.5). The fields are already on
    /// the Madrid clock, so one fixed zone carries them through the formatter unchanged.
    var formatted: String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        let components = DateComponents(
            year: Int(year), month: Int(month), day: Int(day), hour: Int(hour), minute: Int(minute)
        )
        guard let date = calendar.date(from: components) else { return "" }
        return date.formatted(Date.FormatStyle(date: .abbreviated, time: .shortened, timeZone: .gmt))
    }
}

#Preview("Synced") {
    AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview()))
}

#Preview("Never synced") {
    AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview().neverSynced()))
}

#Preview("Loading") {
    AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview().loading()))
}
