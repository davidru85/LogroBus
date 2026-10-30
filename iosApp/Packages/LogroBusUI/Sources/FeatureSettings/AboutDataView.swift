import Shared
import SwiftUI

/// About data (LB-404), opened from More (LB-012): so far, the last successful sync of each dataset (FR-SET-03). The
/// data source, disclaimer and attributions come with LB-404.
public struct AboutDataView: View {
    @State private var viewModel: AboutDataViewModel
    @Environment(\.locale) private var locale

    public init(viewModel: AboutDataViewModel = AboutDataViewModel()) {
        _viewModel = State(initialValue: viewModel)
    }

    public var body: some View {
        content
            .navigationTitle(Text("about.title", bundle: .module))
            // Cancelled when the screen leaves, which stops the use case's collection; restarted by Retry.
            .task(id: viewModel.attempt) { await viewModel.observe() }
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: viewModel.state) {
        case .loading:
            // Spec §7.3: the final layout with placeholders, never a lone spinner.
            lastSyncs(StaticDataset.allCases.map { ($0, Self.placeholderTime) })
                .redacted(reason: .placeholder)
        case .content(let content):
            lastSyncs(content.lastSyncs.map { ($0.dataset, $0.lastSuccess) })
        case .error(let error):
            ContentUnavailableView {
                Text(error.reason.message, bundle: .module)
            } actions: {
                Button {
                    viewModel.onEvent(AboutDataEventRetry.shared)
                } label: {
                    Text("error.retry", bundle: .module)
                }
            }
        }
    }

    private func lastSyncs(_ rows: [(dataset: StaticDataset, lastSuccess: MadridDateTime?)]) -> some View {
        List {
            Section {
                ForEach(rows, id: \.dataset) { row in
                    LabeledContent {
                        if let lastSuccess = row.lastSuccess {
                            Text(lastSuccess.formatted(locale: locale))
                        } else {
                            Text("about.last_sync.never", bundle: .module)
                        }
                    } label: {
                        Text(row.dataset.label, bundle: .module)
                    }
                }
            } header: {
                Text("about.last_sync.title", bundle: .module)
            }
        }
    }

    /// Sample content for the redacted first load.
    private static let placeholderTime = MadridDateTime(year: 2026, month: 1, day: 1, hour: 0, minute: 0)
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

extension ErrorReason {
    /// FR-ERR-05: the short reason (design_guidelines.md §9, keys `error.*`).
    var message: LocalizedStringKey {
        switch self {
        case .network: "error.network"
        case .server: "error.server"
        case .data: "error.data"
        }
    }
}

extension MadridDateTime {
    /// The time in `locale`, as a medium date and a short time (AGENTS.md §5.5). The fields are already on the Madrid
    /// clock, so one fixed zone carries them through the formatter unchanged, whatever the device's zone.
    func formatted(locale: Locale) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        let components = DateComponents(
            year: Int(year), month: Int(month), day: Int(day), hour: Int(hour), minute: Int(minute)
        )
        guard let date = calendar.date(from: components) else { return "" }
        return date.formatted(Date.FormatStyle(date: .abbreviated, time: .shortened, locale: locale, timeZone: .gmt))
    }
}

// Inside a NavigationStack, as More pushes it, so the previews show its title (LB-012).
#Preview("Synced") {
    NavigationStack {
        AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview()))
    }
}

#Preview("Synced, dark") {
    NavigationStack {
        AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview()))
            .preferredColorScheme(.dark)
    }
}

#Preview("Never synced") {
    NavigationStack {
        AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview().neverSynced()))
    }
}

#Preview("Never synced, dark") {
    NavigationStack {
        AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview().neverSynced()))
            .preferredColorScheme(.dark)
    }
}

#Preview("Loading") {
    NavigationStack {
        AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview().loading()))
    }
}

#Preview("Loading, dark") {
    NavigationStack {
        AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview().loading()))
            .preferredColorScheme(.dark)
    }
}

#Preview("Error") {
    NavigationStack {
        AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview().failing()))
    }
}

#Preview("Error, dark") {
    NavigationStack {
        AboutDataView(viewModel: AboutDataViewModel(useCases: SharedDependencies.shared.preview().failing()))
            .preferredColorScheme(.dark)
    }
}
