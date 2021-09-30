//
//  BreedListView.swift
//  KaMPKitiOS
//
//  Created by Russell Wolf on 7/26/21.
//  Copyright © 2021 Touchlab. All rights reserved.
//

import SwiftUI
import shared

// swiftlint:disable force_cast
private let log = koin.get(objCClass: Kermit.self, parameter: "ViewController") as! Kermit

class ObservableBreedModel: ObservableObject {
    private var viewModel: NativeViewModel?

    @Published
    var loading = false

    @Published
    var breeds: [Breed]?

    @Published
    var error: String?

    func activate() {
        viewModel = NativeViewModel(
            onSuccess: { [weak self] success in
                self?.loading = success.loading
                let items = success.data?.allItems
                let count = items?.count ?? 0
                self?.breeds = items
                log.d(withMessage: {"View updating with \(count) breeds"})
                self?.error = nil
            },
            onError: { [weak self] error in
                self?.loading = error.loading
                self?.error = error.exception
                log.e(withMessage: {"Displaying error: \(error.exception)"})
            },
            onEmpty: { [weak self] empty in
                self?.loading = empty.loading
                self?.error = nil
            },
            onLoading: { [weak self] in
                self?.loading = true
            }
        )
    }

    func deactivate() {
        viewModel?.onDestroy()
        viewModel = nil
    }

    func onBreedFavorite(_ breed: Breed) {
        viewModel?.updateBreedFavorite(breed: breed)
    }

    func refresh() {
        viewModel?.refreshBreeds(forced: true)
    }
}

struct BreedListScreen: View {
    @ObservedObject
    var observableModel = ObservableBreedModel()

    var body: some View {
        BreedListContent(
            loading: observableModel.loading,
            breeds: observableModel.breeds,
            error: observableModel.error,
            onBreedFavorite: { observableModel.onBreedFavorite($0) },
            refresh: { observableModel.refresh() }
        )
        .onAppear(perform: {
            observableModel.activate()
        })
        .onDisappear(perform: {
            observableModel.deactivate()
        })
    }
}

struct BreedListContent: View {
    var loading: Bool
    var breeds: [Breed]?
    var error: String?
    var onBreedFavorite: (Breed) -> Void
    var refresh: () -> Void

    var body: some View {
        ZStack {
            VStack {
                if let breeds = breeds {
                    List(breeds, id: \.id) { breed in
                        BreedRowView(breed: breed) {
                            onBreedFavorite(breed)
                        }
                    }
                }
                if let error = error {
                    Text(error)
                        .foregroundColor(.red)
                }
                if (breeds == nil || error == nil) {
                    Spacer()
                }
                Button("Refresh") {
                    refresh()
                }
            }
            if loading { Text("Loading...") }
        }
    }
}

struct BreedRowView: View {
    var breed: Breed
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                Text(breed.name)
                    .padding(4.0)
                Spacer()
                Image(systemName: (breed.favorite == 0) ? "heart" : "heart.fill")
                    .padding(4.0)
            }
        }
    }
}

struct BreedListScreen_Previews: PreviewProvider {
    static var previews: some View {
        BreedListContent(
            loading: false,
            breeds: [
                Breed(id: 0, name: "appenzeller", favorite: 0),
                Breed(id: 1, name: "australian", favorite: 1)
            ],
            error: nil,
            onBreedFavorite: { _ in },
            refresh: {}
        )
    }
}
