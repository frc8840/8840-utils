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

    //check if there's internet connection
    //if there is, add a button to import a library
    //else, add a button to import a library from a file
    let internet = false;

    const req = await fetch("google.com", {
        method: "head",
        mode: "no-cors",
        cache: "no-cache",
        timeout: 3000
    });
    if (req.ok) {
        internet = true;
        console.log("internet connection found")
    } else {
        console.log("no internet connection found", req)
    }

    if (internet) {
        const addLibrarySection = document.createElement("div");
        addLibrarySection.className = "library-element";

        const button_ = document.createElement("button");
        button_.textContent = "Import Library";
        button_.addEventListener("click", openImportLibrary);
        
        addLibrarySection.appendChild(button_);
        libraryContainer.appendChild(addLibrarySection);
    }
}

async function openImportLibrary() {

}

formatLibraries();