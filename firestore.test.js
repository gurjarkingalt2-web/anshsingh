const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- Unauthenticated tests ---
test("Unauthenticated user: cannot read tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
});

test("Unauthenticated user: cannot create task", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
    userId: ALICE_UID,
    title: "Urgent Meeting",
    priority: "urgent",
    isCompleted: false,
    createdAt: new Date(),
  }));
});

// --- Cross-user isolation tests ---
test("Cross-user: Alice cannot read Bob's tasks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("tasks").doc("bob_task").set({
      userId: BOB_UID,
      title: "Secret Task",
      priority: "urgent",
      isCompleted: false,
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("bob_task").get());
});

test("Cross-user: Alice cannot write into Bob's tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("bad_task").set({
    userId: BOB_UID,
    title: "Hacked Task",
    priority: "urgent",
    isCompleted: false,
    createdAt: new Date(),
  }));
});

// --- Owner operations tests ---
test("Owner: Alice can create and read her own task", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1");
  await assertSucceeds(taskRef.set({
    userId: ALICE_UID,
    title: "Call Ansh Singh",
    priority: "urgent",
    category: "call",
    isCompleted: false,
    createdAt: new Date(),
  }));

  await assertSucceeds(taskRef.get());
});

test("Owner: Alice can update task status and title but cannot change createdAt", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      userId: ALICE_UID,
      title: "Initial Title",
      priority: "high",
      isCompleted: false,
      createdAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1");

  // Valid update
  await assertSucceeds(taskRef.update({
    title: "Updated Title",
    isCompleted: true,
  }));

  // Invalid update: attempting to alter createdAt
  await assertFails(taskRef.update({
    createdAt: new Date(Date.now() + 10000),
  }));
});

test("Owner: Alice can create schedule item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const scheduleRef = aliceDb.collection("users").doc(ALICE_UID).collection("schedules").doc("s1");
  await assertSucceeds(scheduleRef.set({
    userId: ALICE_UID,
    title: "Team Standup",
    startTime: "10:00 AM",
    date: "2026-10-22",
    priority: "high",
    isCompleted: false,
    createdAt: new Date(),
  }));
});

test("Owner: Alice can create voice history log", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const historyRef = aliceDb.collection("users").doc(ALICE_UID).collection("history").doc("h1");
  await assertSucceeds(historyRef.set({
    userId: ALICE_UID,
    commandText: "Hey Jarvis, YouTube kholo",
    responseText: "Opening YouTube for you right now.",
    language: "hi",
    actionType: "open_app",
    createdAt: new Date(),
  }));
});
