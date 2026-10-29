import SwiftUI

/// More (FR-NAV-04): Settings and About data in the MVP; News, Fares, Recharge points and Info join in v1.0
/// (design_guidelines.md §2.1). Its entries belong to other features, so the shell owns it.
struct MoreView: View {
    var body: some View {
        List {
            NavigationLink(value: Route.settings) {
                Text("more.settings")
            }
            NavigationLink(value: Route.aboutData) {
                Text("more.about_data")
            }
        }
        .navigationTitle(Text("more.title"))
    }
}

#Preview {
    NavigationStack {
        MoreView()
    }
}
