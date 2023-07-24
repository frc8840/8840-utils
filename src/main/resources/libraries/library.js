const libraryContainer = document.getElementsByClassName("libraries")[0];

const apiEndpoint = "/api/libraries"

async function getLibraries() {
    const response = await fetch(apiEndpoint);
    const libraries = await response.json();
    return libraries;
}

async function formatLibraries() {
    const libs = await getLibraries();

    for (let i = 0; i < libs.count; i++) {
        const lib = libs.libraries[i];

        const libBox = document.createElement("div");

        libBox.className = "library-element";

        const title = document.createElement("h3");
        title.textContent = lib.name;

        const subtitle = document.createElement("h4");
        subtitle.textContent = `${lib.author} • ${lib.repo}`

        const description = document.createElement("p");
        description.textContent = lib.description;

        libBox.appendChild(title);
        libBox.appendChild(description);

        libraryContainer.appendChild(libBox);

    }
}

formatLibraries();