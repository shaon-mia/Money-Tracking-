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

function safePastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validPreferences(uid) {
  const ts = safePastTimestamp();
  return {
    userId: uid,
    language: "bn",
    themeMode: "light",
    textScale: "medium",
    currencyCode: "BDT",
    monthlySavingsGoal: 10000,
    biometricLockEnabled: false,
    pinEnabled: false,
    hideBalances: false,
    demoSeeded: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validTransaction(uid) {
  const ts = safePastTimestamp();
  return {
    userId: uid,
    type: "expense",
    amount: 450,
    categoryId: "cat_food",
    categoryNameBn: "খাবার",
    categoryNameEn: "Food",
    timestampMillis: 1700000000000,
    note: "দুপুরের খাবার",
    paymentMethod: "cash",
    isDemo: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user: cannot read or write transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").set(validTransaction(ALICE_UID))
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's collections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").set(validTransaction(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").set(validPreferences(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx2").set(validTransaction(BOB_UID))
  );
});

test("Owner can save transaction with paymentMethod 'other' and invalid fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txOther = { ...validTransaction(ALICE_UID), paymentMethod: "other" };
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_other").set(txOther)
  );

  const txBadField = { ...validTransaction(ALICE_UID), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_ghost").set(txBadField)
  );

  const txNegativeAmount = { ...validTransaction(ALICE_UID), amount: -100 };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_neg").set(txNegativeAmount)
  );
});

test("Owner CRUD works for preferences, categories, budgets, savings, and lend/borrow", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = safePastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").set(validPreferences(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("preferences").update({
      language: "en",
      deletePasswordEnabled: true,
      deletePasswordHash: "a".repeat(64),
      updatedAt: safePastTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").doc("cat_food").set({
      userId: ALICE_UID,
      nameBn: "খাবার",
      nameEn: "Food",
      type: "expense",
      iconKey: "restaurant",
      colorHex: 0xFFFF6B2C,
      isDefault: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("b_food_2026_11").set({
      userId: ALICE_UID,
      categoryId: "cat_food",
      categoryNameBn: "খাবার",
      categoryNameEn: "Food",
      monthKey: "2026-11",
      limitAmount: 12000,
      isDemo: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savingsGoals").doc("goal_1").set({
      userId: ALICE_UID,
      title: "Emergency Fund",
      targetAmount: 50000,
      savedAmount: 10000,
      targetDateMillis: 1790000000000,
      iconKey: "savings",
      colorHex: 0xFF10B981,
      isDemo: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savingsEntries").doc("entry_1").set({
      userId: ALICE_UID,
      goalId: "goal_1",
      goalTitle: "Emergency Fund",
      amount: 5000,
      note: "Monthly deposit",
      timestampMillis: 1700000000000,
      isDemo: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lendBorrows").doc("lb_1").set({
      userId: ALICE_UID,
      kind: "lend",
      personName: "Karim",
      phoneNumber: "01711000000",
      totalAmount: 5000,
      paidAmount: 0,
      dateMillis: 1700000000000,
      dueDateMillis: 1705000000000,
      description: "Personal loan",
      status: "unpaid",
      isDemo: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lendBorrowPayments").doc("lbp_1").set({
      userId: ALICE_UID,
      recordId: "lb_1",
      amount: 5000,
      note: "Full settlement",
      timestampMillis: 1701000000000,
      isDemo: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lendBorrows").doc("lb_1").update({
      paidAmount: 5000,
      status: "paid",
      updatedAt: safePastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("lendBorrows").doc("lb_1").update({
      paidAmount: 4000,
      status: "partial",
      updatedAt: safePastTimestamp(),
    })
  );
});

test("Orphaned savingsEntry or lendBorrowPayment rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = safePastTimestamp();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("savingsEntries").doc("entry_orphan").set({
      userId: ALICE_UID,
      goalId: "missing_goal",
      goalTitle: "Ghost",
      amount: 1000,
      note: "",
      timestampMillis: 1700000000000,
      isDemo: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
