const path = "/files"

class FileSystem {
    static instance = null;

    static {
        console.log("[FileSystem] FileSystem is initializing...")

        FileSystem.instance = new FileSystem();

        window.fs = FileSystem.instance;
    }

    static FileType = {
        Log: "logs",
        Path: "paths"
    }

    constructor() {
        this.logfiles = {};
        this.pathfiles = {};

        this.folderBase = "";
    }

    getAllFiles() {
        const files = [];
        for (const key in this.logfiles) {
            const file = this.logfiles[key];
            const copy = Object.assign(Object.assign({}, file), {
                path: "8840applogs/" + file.path
            })
            files.push(copy);
        }

        for (const key in this.pathfiles) {
            const file = this.pathfiles[key];
            const copy = Object.assign(Object.assign({}, file), {
                path: "8840appdata/" + file.path
            })
            files.push(copy);
        }

        return files;
    }

    async updateFiles() {
        const rawLogFiles = await this.requestFiles(FileSystem.FileType.Log);
        const rawPathFiles = await this.requestFiles(FileSystem.FileType.Path);

        this.logfiles = {};
        this.pathfiles = {};

        for (const file of rawLogFiles) {
            const name = file.name;
            const size = file.size;
            const rawPath = file.path;

            const home = rawPath.split("8840applogs/")[0];
            const path = rawPath.substring(home.length + "8840applogs/".length);

            this.logfiles[path] = {
                name: name,
                size: size,
                path: path
            }

            this.folderBase = home;
        }

        for (const file of rawPathFiles) {
            const name = file.name;
            const size = file.size;
            const rawPath = file.path;

            const home = rawPath.split("8840appdata/")[0];
            const path = rawPath.substring(home.length + "8840appdata/".length);

            this.pathfiles[path] = {
                name: name,
                size: size,
                path: path
            }

            this.folderBase = home;
        }
    }
    async requestFiles(type=FileSystem.FileType.Log) {
        const base = "http://" + document.getElementById("ip-addr").value + ":5805" + path;

        const req = await fetch(base, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                folder: type
            })
        });

        // if (req.status !== 200) {
        //     throw new Error("Failed to fetch files");
        // }
        
        const files = await req.json();

        if (!files.success) throw new Error("Failed to fetch files");

        return files.files;
    }

    async writeFile(_path, rawData) {
        const base = "http://" + window.nt.host + ":" + window.nt.port + path;

        const base64Data = btoa(rawData);

        const req = await fetch(base, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                path: _path,
                data: base64Data
            })
        });

        const data = await req.json();

        if (!data.success) throw new Error("Failed to write files");

        return data;
    }

    async readFile(_path) {
        console.log("opening " + _path + "...")
        const base = "http://" + document.getElementById("ip-addr").value + ":5805" + path;

        const req = await fetch(base, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                path: _path
            })
        });

        const data = await req.json();
        
        if (!data.success) throw new Error("Failed to read files");

        return data.data;
    }

    async mkdir(_path) {
        const base = "http://" + window.nt.host + ":" + window.nt.port + path;

        const req = await fetch(base, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                path: _path,
                data: "FOLDER"
            })
        });

        const data = await req.json();

        if (!data.success) throw new Error("Failed to create folder");

        return data;
    }
}


class Finder {
    constructor() {
        const f = async () => {
            console.log("[Finder] Getting Files...")
            await window.fs.updateFiles();
            
            // const homeFolder = document.getElementById("home-folder");
            // homeFolder.textContent = window.fs.folderBase;
            
            console.log("in folder is", this.state.in_folder);

            this.forceUpdate();
        }

        this.state = {
            in_folder: []
        }

        setTimeout(() => {
            f();
        }, 100);

        window.changeFolder = (folder) => {
            if (folder == "..") {
                this.state.in_folder.pop();
            } else {
                this.state.in_folder.push(folder);
            }

            this.forceUpdate();
        }

        window.openFile = async (file) => {
            const raw = await window.fs.readFile(file);
            const data = atob(raw);

            const name = file.split("/")[file.split("/").length - 1];

            //Open in about:blank
            const win = window.open("about:blank", "_blank");

            win.document.write(`
                <html>
                    <head>
                        <title>${name}</title>
                        <style>
                            body {
                                background-color: #1e1e1e;
                                color: #fff;
                                font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
                                overflow-y:visible;
                                width: 100vw;
                                font-size: 12px;
                            }

                            pre {
                                white-space: pre-wrap;
                                word-wrap: break-word;
                                overflow-y: visible;
                            }
                        </style>
                    </head>
                    <body>
                        <pre>${data}</pre>
                    </body>
                </html>
            `);
        }

    }
    getFilesInFolder() {
        //This goes through the files and returns the ones that are in the current folder
        if (this.state.in_folder.length == 0) {
            //return the folders
            return [
                {
                    name: "logs",
                    size: Object.keys(window.fs.logfiles).length + " files",
                    path: "8840applogs",
                    folder: true,
                    back: false
                },
                {
                    name: "paths",
                    size: Object.keys(window.fs.pathfiles).length + " files",
                    path: "8840appdata",
                    folder: true,
                    back: false,
                },
            ]
        }

        const files = [
            
        ];

        const folders = new Set();

        let hasDepthOfNext = false;
        
        const greaterDepths = [];

        //Go through the files and return the ones that are in the current folder
        for (const file of window.fs.getAllFiles()) {
            if (!file.path.startsWith(this.state.in_folder.join("/") + "/")) {
                continue;
            }

            const path = file.path.split("/");

            const depth = path.length - 2;
            const folder = path[depth];

            if (depth < this.state.in_folder.length - 1) {
                console.log(depth, path);
                continue;
            }

            if (depth == this.state.in_folder.length) {
                folders.add(folder);

                continue;
            }

            if (depth > this.state.in_folder.length) {
                greaterDepths.push(file);
                hasDepthOfNext = true;
                continue;
            }
            
            if (this.state.in_folder.length == 1) {
                files.push(Object.assign(file, {
                    size: file.size + " bytes",
                }));
                continue;
            }

            if (folder == this.state.in_folder[this.state.in_folder.length - 1]) {
                files.push(file);
            }
        }

        if (hasDepthOfNext) {
            for (const file of greaterDepths) {
                const path = file.path.split("/");
                const depth = path.length - 2;

                if (depth > this.state.in_folder.length - 1) {
                    folders.add(path[this.state.in_folder.length]);
                }
            }
        }

        const formattedFolders = [];

        for (const folder of folders) {
            formattedFolders.push({
                name: folder,
                size: "-",
                path: folder,
                folder: true,
                back: false
            });
        }

        const concatFiles = [
            {
                name: "..",
                size: "-",
                path: "..",
                folder: false,
                back: true
            },
            ...formattedFolders,
            ...files
        ]

        return concatFiles;
    }

    sendFile() {
        if (this.state.in_folder.length == 0) {
            alert("You cannot send files from the root directory.")
            return;
        }

        //Create a file input
        const input = document.createElement("input");
        input.type = "file";
        input.style.display = "none";

        input.onchange = async (e) => {
            //Get file name
            const file = e.target.files[0];
            const name = file.name;

            //get file type
            const type = file.type;

            const nameChange = prompt("What would you like to name the file?", name);

            if (nameChange == null) {
                return;
            }

            //Get file data
            const reader = new FileReader();
            reader.readAsArrayBuffer(file);

            reader.onload = async (e) => {
                const data = e.target.result;

                console.log("Writing to " + this.state.in_folder.join("/") + "/" + nameChange + "...");

                //convert data to string
                const dataString = String.fromCharCode.apply(null, new Uint8Array(data));

                //Send file
                await window.fs.writeFile(
                    this.state.in_folder.join("/") + "/" + nameChange, 
                    //String
                    dataString,
                );

                //Update files
                await window.fs.updateFiles();

                //Update UI
                this.forceUpdate();
            }
        }

        input.click();
        input.remove();
    }

    forceUpdate() {
        this.render();
    }

    render() {
        const fileListDOM = document.querySelector(".file-list");
        fileListDOM.innerHTML = "";

        for (let file of this.getFilesInFolder()) {
            const newDiv = document.createElement("div");

            newDiv.textContent = file.name;
            newDiv.dataset.name = file.name;
            newDiv.dataset.size = file.size;
            newDiv.dataset.path = file.path;
            newDiv.dataset.folder = file.folder;
            newDiv.dataset.back = file.back;

            newDiv.onclick = async (e) => {
                if (e.target.dataset.folder == "true") {
                    window.changeFolder(e.target.dataset.path);
                } else if (e.target.dataset.back == "true") {
                    window.changeFolder("..");
                } else {
                    window.openFile(e.target.dataset.path);
                }

                this.forceUpdate();
            };

            fileListDOM.appendChild(newDiv);
        }
    }
}

new Finder().render();