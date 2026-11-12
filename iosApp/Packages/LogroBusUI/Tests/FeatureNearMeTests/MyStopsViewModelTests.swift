@testable import FeatureNearMe
import Shared
import Testing

/// LB-208 part 4 (FR-HOME-02, FR-FAV-02) on iOS: "My stops" follows the saved stops, and its edit actions reach the
/// repository in the order and with the alias the user made.
@MainActor
struct MyStopsViewModelTests {
    @Test
    func lb208_theSectionIsEmptyUntilAStopIsSaved() async throws {
        let viewModel = MyStopsViewModel(useCases: SharedDependencies.shared.preview())

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }

        try await waitUntil { viewModel.rows.isEmpty }
        #expect(viewModel.rows.isEmpty)
    }

    @Test
    func lb208_aSavedStopShowsUnderItsAlias_inTheUsersOrder() async throws {
        let useCases = SharedDependencies.shared.preview()
        let saved = useCases.savedStops()
        _ = try? await saved.add(stopId: 38, alias: "Casa")
        _ = try? await saved.add(stopId: 61, alias: "Trabajo")
        let viewModel = MyStopsViewModel(useCases: useCases)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }

        try await waitUntil { viewModel.rows.count == 2 }
        #expect(viewModel.rows.map(\.stopId) == [38, 61])
        #expect(viewModel.rows.map(\.label) == ["Casa", "Trabajo"])
    }

    @Test
    func lb208_reorderingSendsTheUsersOrder() async throws {
        let useCases = SharedDependencies.shared.preview()
        let saved = useCases.savedStops()
        _ = try? await saved.add(stopId: 38, alias: "Casa")
        _ = try? await saved.add(stopId: 61, alias: "Trabajo")
        let viewModel = MyStopsViewModel(useCases: useCases)

        await viewModel.reorder([KotlinInt(int: 61), KotlinInt(int: 38)])

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.rows.map(\.stopId) == [61, 38] }
    }

    @Test
    func lb208_renamingKeepsTheStopSaved_underTheNewAlias() async throws {
        let useCases = SharedDependencies.shared.preview()
        let saved = useCases.savedStops()
        _ = try? await saved.add(stopId: 38, alias: "Casa")
        let viewModel = MyStopsViewModel(useCases: useCases)

        await viewModel.rename(stopId: 38, alias: "Piso")

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.rows.map(\.label) == ["Piso"] }
    }

    @Test
    func lb208_deletingRemovesTheStop() async throws {
        let useCases = SharedDependencies.shared.preview()
        let saved = useCases.savedStops()
        _ = try? await saved.add(stopId: 38, alias: "Casa")
        _ = try? await saved.add(stopId: 61, alias: "Trabajo")
        let viewModel = MyStopsViewModel(useCases: useCases)

        await viewModel.delete(stopId: 38)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.rows.map(\.stopId) == [61] }
    }

    @Test
    func lb208_theOrderHelperNeverMovesPastEitherEnd() {
        let rows = [
            savedRow(38, "Casa", 0),
            savedRow(61, "Trabajo", 1)
        ]

        #expect(rows.moved(0, -1).map(\.intValue) == [38, 61])
        #expect(rows.moved(1, 1).map(\.intValue) == [38, 61])
        #expect(rows.moved(0, 1).map(\.intValue) == [61, 38])
        #expect([SavedStopRowUi]().moved(0, 1).isEmpty)
    }

    private func savedRow(_ stopId: Int32, _ label: String, _ position: Int32) -> SavedStopRowUi {
        SavedStopRowUi(stopId: stopId, label: label, position: position)
    }
}
